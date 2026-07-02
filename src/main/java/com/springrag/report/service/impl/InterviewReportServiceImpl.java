package com.springrag.report.service.impl;

import com.springrag.common.ai.ChatOptionsPresets;
import com.springrag.common.exception.ResourceNotFoundException;
import com.springrag.common.tx.TransactionRunner;
import com.springrag.interview.domain.InterviewAnswer;
import com.springrag.interview.domain.InterviewQuestion;
import com.springrag.interview.domain.InterviewSession;
import com.springrag.interview.domain.enums.InterviewStatus;
import com.springrag.interview.event.InterviewCompletedEvent;
import com.springrag.interview.repository.InterviewAnswerRepository;
import com.springrag.interview.repository.InterviewQuestionRepository;
import com.springrag.interview.repository.InterviewSessionRepository;
import com.springrag.config.AsyncConfig;
import com.springrag.report.dto.InterviewReportResponse;
import com.springrag.report.dto.InterviewReportResult;
import com.springrag.report.service.InterviewReportService;
import com.springrag.skill.service.SkillTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Generates an interview report — the one-to-one outcome of an interview, stored directly on the
 * {@link InterviewSession}. The LLM call runs OUTSIDE any transaction: inputs are read first, the
 * model is called with no session held, then the report fields + completion are persisted
 * atomically via {@link TransactionRunner}.
 */
@Service
public class InterviewReportServiceImpl implements InterviewReportService {

    private static final Logger log = LoggerFactory.getLogger(InterviewReportServiceImpl.class);

    private final ChatClient chatClient;
    private final InterviewSessionRepository interviewRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final TransactionRunner transactionRunner;
    private final SkillTrackingService skillTrackingService;

    public InterviewReportServiceImpl(ChatClient.Builder chatClientBuilder,
                                      InterviewSessionRepository interviewRepository,
                                      InterviewQuestionRepository questionRepository,
                                      InterviewAnswerRepository answerRepository,
                                      TransactionRunner transactionRunner,
                                      SkillTrackingService skillTrackingService) {
        this.chatClient = chatClientBuilder.build();
        this.interviewRepository = interviewRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.transactionRunner = transactionRunner;
        this.skillTrackingService = skillTrackingService;
    }

    /**
     * Lifecycle hand-off: when an interview completes, generate its report and aggregate skills on a
     * background thread (the LLM call must not block the final interview turn). Failures are logged,
     * not propagated — the report can be regenerated via the REST endpoint.
     */
    @Async(AsyncConfig.AI_EXECUTOR)
    @EventListener
    public void onInterviewCompleted(InterviewCompletedEvent event) {
        try {
            log.info("Handling InterviewCompletedEvent. interviewId={}", event.getInterviewId());
            generateReport(event.getInterviewId());
        } catch (Exception ex) {
            log.error("Automatic report generation failed for interviewId={}", event.getInterviewId(), ex);
        }
    }

    @Override
    public InterviewReportResponse generateReport(UUID interviewId) {
        InterviewSession interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + interviewId));
        List<InterviewQuestion> questions =
                questionRepository.findByInterviewIdOrderBySequenceNumberAsc(interviewId);
        List<InterviewAnswer> answers = answerRepository.findByQuestionInterviewId(interviewId);

        String prompt = buildPrompt(interview, questions, answers);

        // Model call runs with no transaction held. Deterministic so the same interview
        // yields a stable overall score and report.
        InterviewReportResult result = chatClient.prompt(prompt)
                .options(ChatOptionsPresets.deterministic())
                .call()
                .entity(InterviewReportResult.class);

        transactionRunner.run(() -> {
            interview.setOverallScore(result.getOverallScore());
            interview.setReportStrengths(result.getStrengths());
            interview.setReportWeaknesses(result.getWeaknesses());
            interview.setReportRecommendations(result.getRecommendations());
            interview.setReportSummary(result.getSummary());
            interview.setReportGeneratedAt(LocalDateTime.now());
            interview.setStatus(InterviewStatus.COMPLETED);
            interview.setCompletedAt(LocalDateTime.now());
            interviewRepository.save(interview);
        });

        // Now that the overall score is committed, fold it into the candidate's running skill
        // averages. This is the link that revives the skill-progression / coaching loop.
        skillTrackingService.updateSkills(interviewId);

        log.info("Interview report generated and skills updated. interviewId={}", interviewId);
        return toResponse(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewReportResponse getReport(UUID interviewId) {
        InterviewSession interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + interviewId));
        if (interview.getReportGeneratedAt() == null) {
            throw new ResourceNotFoundException("Report not generated for interview: " + interviewId);
        }
        return toResponse(interview);
    }

    private String buildPrompt(InterviewSession interview,
                               List<InterviewQuestion> questions,
                               List<InterviewAnswer> answers) {
        StringBuilder prompt = new StringBuilder("""
                You are a senior technical interview coach.

                Analyze the entire interview.

                Generate:

                overallScore
                strengths
                weaknesses
                recommendations
                summary

                Interview Type:
                """)
                .append(interview.getType())
                .append("\n\nDifficulty:\n")
                .append(interview.getDifficulty())
                .append("\n\n");

        for (int i = 0; i < questions.size(); i++) {
            prompt.append("Question: ").append(questions.get(i).getQuestion()).append("\n");
            if (i < answers.size()) {
                InterviewAnswer answer = answers.get(i);
                prompt.append("Answer: ").append(answer.getAnswer()).append("\n");
                prompt.append("Score: ").append(answer.getScore()).append("\n");
                prompt.append("Strengths: ").append(answer.getStrengths()).append("\n");
                prompt.append("Weaknesses: ").append(answer.getWeaknesses()).append("\n");
            }
            prompt.append("\n");
        }
        return prompt.toString();
    }

    private InterviewReportResponse toResponse(InterviewSession interview) {
        InterviewReportResponse response = new InterviewReportResponse();
        response.setInterviewId(interview.getId());
        response.setOverallScore(interview.getOverallScore());
        response.setStrengths(interview.getReportStrengths());
        response.setWeaknesses(interview.getReportWeaknesses());
        response.setRecommendations(interview.getReportRecommendations());
        response.setSummary(interview.getReportSummary());
        return response;
    }
}
