package com.springrag.skill.service.impl;

import com.springrag.common.exception.ResourceNotFoundException;
import com.springrag.interview.domain.InterviewSession;
import com.springrag.interview.domain.enums.InterviewType;
import com.springrag.interview.repository.InterviewSessionRepository;
import com.springrag.skill.domain.SkillCategory;
import com.springrag.skill.domain.UserSkill;
import com.springrag.skill.dto.UserSkillResponse;
import com.springrag.skill.repository.UserSkillRepository;
import com.springrag.skill.service.SkillTrackingService;
import com.springrag.user.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SkillTrackingServiceImpl implements SkillTrackingService {

    private static final Logger log = LoggerFactory.getLogger(SkillTrackingServiceImpl.class);

    private final InterviewSessionRepository interviewRepository;
    private final UserSkillRepository skillRepository;

    public SkillTrackingServiceImpl(InterviewSessionRepository interviewRepository,
                                    UserSkillRepository skillRepository) {
        this.interviewRepository = interviewRepository;
        this.skillRepository = skillRepository;
    }

    @Override
    public void updateSkills(UUID interviewId) {
        log.info("Updating skills for interviewId={}", interviewId);

        InterviewSession interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + interviewId));

        if (interview.getOverallScore() == null) {
            throw new ResourceNotFoundException("Report not generated for interview: " + interviewId);
        }

        User user = interview.getUser();
        SkillCategory category = mapSkill(interview.getType());

        UserSkill skill = skillRepository.findByUserIdAndSkillCategory(user.getId(), category)
                .orElseGet(() -> {
                    UserSkill created = new UserSkill();
                    created.setUser(user);
                    created.setSkillCategory(category);
                    created.setAverageScore(0.0);
                    created.setAssessmentCount(0);
                    return created;
                });

        int count = skill.getAssessmentCount();
        double updatedAverage = (skill.getAverageScore() * count + interview.getOverallScore()) / (count + 1);
        skill.setAverageScore(updatedAverage);
        skill.setAssessmentCount(count + 1);
        skillRepository.save(skill);

        log.info("Skill updated. userId={}, category={}, score={}", user.getId(), category, updatedAverage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSkillResponse> getUserSkills(UUID userId) {
        return skillRepository.findByUserIdOrderByAverageScoreDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private SkillCategory mapSkill(InterviewType interviewType) {
        return switch (interviewType) {
            case SPRING_BOOT -> SkillCategory.SPRING_BOOT;
            case MICROSERVICES -> SkillCategory.MICROSERVICES;
            case SYSTEM_DESIGN -> SkillCategory.SYSTEM_DESIGN;
            case BEHAVIORAL, HR -> SkillCategory.BEHAVIORAL;
            default -> SkillCategory.JAVA;
        };
    }

    private UserSkillResponse toResponse(UserSkill skill) {
        UserSkillResponse response = new UserSkillResponse();
        response.setSkillCategory(skill.getSkillCategory());
        response.setAverageScore(skill.getAverageScore());
        response.setAssessmentCount(skill.getAssessmentCount());
        return response;
    }
}
