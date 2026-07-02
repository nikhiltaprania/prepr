package com.springrag.interview.service;

import com.springrag.interview.dto.CreateInterviewAnswerRequest;
import com.springrag.interview.dto.InterviewAnswerResponse;

import java.util.List;
import java.util.UUID;

public interface InterviewAnswerService {

    InterviewAnswerResponse create(
            CreateInterviewAnswerRequest request);

    List<InterviewAnswerResponse>
    getInterviewAnswers(
            UUID interviewId);
}