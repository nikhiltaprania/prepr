package com.springrag.interview.service;

import com.springrag.interview.dto.CreateInterviewRequest;
import com.springrag.interview.dto.InterviewResponse;

import java.util.List;
import java.util.UUID;

public interface InterviewService {

    InterviewResponse create(CreateInterviewRequest request);

    InterviewResponse start(UUID interviewId);

    InterviewResponse getById(UUID interviewId);

    List<InterviewResponse> getUserInterviews(UUID userId);
}
