package com.springrag.interview.service.impl;

import com.springrag.common.exception.ResourceNotFoundException;
import com.springrag.interview.domain.InterviewSession;
import com.springrag.interview.domain.enums.InterviewStatus;
import com.springrag.interview.dto.CreateInterviewRequest;
import com.springrag.interview.dto.InterviewResponse;
import com.springrag.interview.repository.InterviewSessionRepository;
import com.springrag.interview.service.InterviewService;
import com.springrag.user.domain.User;
import com.springrag.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InterviewServiceImpl implements InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewServiceImpl.class);

    private final InterviewSessionRepository interviewRepository;
    private final UserRepository userRepository;

    public InterviewServiceImpl(InterviewSessionRepository interviewRepository, UserRepository userRepository) {
        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
    }

    @Override
    public InterviewResponse create(CreateInterviewRequest request) {
        log.info("Creating interview. userId={}, type={}, difficulty={}",
                request.getUserId(), request.getType(), request.getDifficulty());

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));

        InterviewSession interview = new InterviewSession();
        interview.setUser(user);
        interview.setType(request.getType());
        interview.setDifficulty(request.getDifficulty());
        interview.setAdaptive(request.isAdaptive());
        interview.setStatus(InterviewStatus.CREATED);

        InterviewSession saved = interviewRepository.save(interview);
        log.info("Interview created. id={}", saved.getId());
        return toResponse(saved);
    }

    @Override
    public InterviewResponse start(UUID interviewId) {
        log.info("Starting interview. id={}", interviewId);

        InterviewSession interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + interviewId));

        interview.setStatus(InterviewStatus.IN_PROGRESS);
        interview.setStartedAt(LocalDateTime.now());

        InterviewSession saved = interviewRepository.save(interview);
        log.info("Interview started. id={}", interviewId);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public InterviewResponse getById(UUID interviewId) {
        InterviewSession interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + interviewId));
        return toResponse(interview);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewResponse> getUserInterviews(UUID userId) {
        return interviewRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private InterviewResponse toResponse(InterviewSession interview) {
        InterviewResponse response = new InterviewResponse();
        response.setId(interview.getId());
        response.setUserId(interview.getUser().getId());
        response.setType(interview.getType());
        response.setDifficulty(interview.getDifficulty());
        response.setStatus(interview.getStatus());
        response.setAdaptive(interview.isAdaptive());
        response.setOverallScore(interview.getOverallScore());
        response.setStartedAt(interview.getStartedAt());
        response.setCompletedAt(interview.getCompletedAt());
        return response;
    }
}
