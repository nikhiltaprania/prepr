package com.springrag.coaching.service.impl;

import com.springrag.coaching.domain.CoachingPlan;
import com.springrag.coaching.dto.CoachingPlanResponse;
import com.springrag.coaching.dto.CoachingPlanResult;
import com.springrag.coaching.repository.CoachingPlanRepository;
import com.springrag.coaching.service.CoachingService;
import com.springrag.common.ai.ChatOptionsPresets;
import com.springrag.common.exception.ResourceNotFoundException;
import com.springrag.common.tx.TransactionRunner;
import com.springrag.interview.domain.InterviewSession;
import com.springrag.interview.domain.enums.InterviewStatus;
import com.springrag.interview.repository.InterviewSessionRepository;
import com.springrag.skill.domain.UserSkill;
import com.springrag.skill.repository.UserSkillRepository;
import com.springrag.user.domain.User;
import com.springrag.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Builds a personalized coaching plan grounded in the candidate's REAL performance: profile, current
 * skill averages, and their recent completed interviews (scores + the report's strengths/weaknesses/
 * recommendations) plus the score trend. Read-only data is loaded first, then the LLM call runs with no
 * transaction held (the service is intentionally not {@code @Transactional}); decoding is deterministic
 * so the same performance yields a stable plan.
 */
@Service
public class CoachingServiceImpl implements CoachingService {

    private static final Logger log = LoggerFactory.getLogger(CoachingServiceImpl.class);
    private static final int RECENT_INTERVIEWS = 5;

    private final ChatClient chatClient;
    private final UserRepository userRepository;
    private final UserSkillRepository skillRepository;
    private final InterviewSessionRepository interviewRepository;
    private final CoachingPlanRepository planRepository;
    private final TransactionRunner transactionRunner;

    public CoachingServiceImpl(ChatClient.Builder chatClientBuilder,
                               UserRepository userRepository,
                               UserSkillRepository skillRepository,
                               InterviewSessionRepository interviewRepository,
                               CoachingPlanRepository planRepository,
                               TransactionRunner transactionRunner) {
        this.chatClient = chatClientBuilder.build();
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.interviewRepository = interviewRepository;
        this.planRepository = planRepository;
        this.transactionRunner = transactionRunner;
    }

    @Override
    @Transactional(readOnly = true)
    public CoachingPlanResponse getActivePlan(UUID userId) {
        return planRepository.findByUserId(userId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No coaching plan yet for user: " + userId));
    }

    @Override
    public CoachingPlanResponse generatePlan(UUID userId) {
        log.info("Generating coaching plan for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        List<UserSkill> skills = skillRepository.findByUserIdOrderByAverageScoreDesc(userId);

        // Recent completed, scored interviews (newest-first from the repo) — the evidence base.
        List<InterviewSession> recent = interviewRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(i -> i.getStatus() == InterviewStatus.COMPLETED && i.getOverallScore() != null)
                .limit(RECENT_INTERVIEWS)
                .toList();

        // LLM call runs with no transaction held.
        CoachingPlanResult result = chatClient.prompt(buildPrompt(user, skills, recent))
                .options(ChatOptionsPresets.deterministic())
                .call()
                .entity(CoachingPlanResult.class);

        // Upsert the single active plan for this user (replace in place).
        CoachingPlan saved = transactionRunner.call(() -> {
            CoachingPlan plan = planRepository.findByUserId(userId).orElseGet(CoachingPlan::new);
            plan.setUserId(userId);
            plan.setFocusAreas(result.getFocusAreas());
            plan.setRecommendations(result.getRecommendations());
            plan.setPracticePlan(result.getPracticePlan());
            plan.setEstimatedDuration(result.getEstimatedDuration());
            return planRepository.save(plan);
        });
        return toResponse(saved);
    }

    private CoachingPlanResponse toResponse(CoachingPlan plan) {
        CoachingPlanResponse response = new CoachingPlanResponse();
        response.setUserId(plan.getUserId());
        response.setFocusAreas(plan.getFocusAreas());
        response.setRecommendations(plan.getRecommendations());
        response.setPracticePlan(plan.getPracticePlan());
        response.setEstimatedDuration(plan.getEstimatedDuration());
        return response;
    }

    private String buildPrompt(User user, List<UserSkill> skills, List<InterviewSession> recent) {
        StringBuilder prompt = new StringBuilder("""
                You are an expert technical interview coach. Build a personalized plan grounded ONLY in
                the candidate's real data below — their skill scores, recent interview outcomes, and the
                strengths/weaknesses identified by their interview reports. Be specific; reference their
                actual weak areas and scores. Do not invent generic advice.

                === Candidate profile ===
                """)
                .append("Target role: ").append(orNa(user.getTargetRole())).append("\n")
                .append("Experience: ").append(user.getExperienceYears() == null ? "n/a" : user.getExperienceYears() + " years").append("\n\n")
                .append("=== Current skill averages (0-10, weakest first) ===\n");

        if (skills.isEmpty()) {
            prompt.append("(no skill data yet)\n");
        } else {
            skills.stream()
                    .sorted(Comparator.comparingDouble(s -> safe(s.getAverageScore())))
                    .forEach(s -> prompt.append("- ").append(s.getSkillCategory())
                            .append(": ").append(String.format("%.1f", safe(s.getAverageScore())))
                            .append(" (").append(s.getAssessmentCount() == null ? 0 : s.getAssessmentCount()).append(" interviews)\n"));
        }

        prompt.append("\n=== Recent interviews (newest first) ===\n");
        if (recent.isEmpty()) {
            prompt.append("(no completed interviews yet — give foundational guidance for the target role)\n");
        } else {
            int n = 1;
            // Score trend oldest→newest for context.
            List<Double> trend = new ArrayList<>(recent.stream().map(InterviewSession::getOverallScore).toList());
            java.util.Collections.reverse(trend);
            for (InterviewSession i : recent) {
                prompt.append(n++).append(". ").append(i.getType()).append(" / ").append(i.getDifficulty())
                        .append(" — score ").append(String.format("%.1f", i.getOverallScore())).append("/10\n");
                appendIf(prompt, "   Strengths: ", i.getReportStrengths());
                appendIf(prompt, "   Weaknesses: ", i.getReportWeaknesses());
                appendIf(prompt, "   Recommendations: ", i.getReportRecommendations());
            }
            prompt.append("Score trend (oldest→newest): ")
                    .append(trend.stream().map(s -> String.format("%.1f", s)).toList()).append("\n");
        }

        return prompt.append("""

                === Produce ===
                focusAreas — the candidate's weakest, highest-impact areas (cite their actual scores/weaknesses)
                recommendations — concrete, specific actions tied to those gaps
                practicePlan — a sequenced plan (what to practice, in what order)
                estimatedDuration — a realistic time estimate

                Prioritize the weakest skills and the recurring weaknesses across their recent interviews.
                """).toString();
    }

    private static void appendIf(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) sb.append(label).append(value.strip()).append("\n");
    }

    private static String orNa(String v) { return (v == null || v.isBlank()) ? "n/a" : v; }

    private static double safe(Double value) { return value != null ? value : 0.0; }
}
