package com.springrag.interview.service;

import com.springrag.interview.dto.CreateInterviewQuestionRequest;
import com.springrag.interview.dto.InterviewQuestionResponse;

import java.util.List;
import java.util.UUID;

public interface InterviewQuestionService {

    /** Authors a manual question in the interview plan, at its configured sequence number. */
    InterviewQuestionResponse create(CreateInterviewQuestionRequest request);

    /** Generates {@code count} AI questions and appends them to the plan, in order (not yet asked). */
    List<InterviewQuestionResponse> generatePlan(UUID interviewId, int count);

    /**
     * The next question that should be asked, WITHOUT marking it asked. Returns the next planned
     * (un-asked) question; if the plan is exhausted and the session is adaptive, generates one and
     * parks it (un-asked); empty when a scripted interview has no questions left.
     */
    java.util.Optional<InterviewQuestionResponse> peekNextQuestion(UUID interviewId);

    /** Marks a question as asked (presented to the candidate). */
    InterviewQuestionResponse markAsked(UUID questionId);

    /**
     * Returns the next question to ask, marking it asked. Asks planned questions in sequence order;
     * once they are exhausted it generates an adaptive follow-up only if the session is adaptive,
     * otherwise it reports the interview as completed.
     */
    InterviewQuestionResponse nextQuestion(UUID interviewId);

    List<InterviewQuestionResponse> getInterviewQuestions(UUID interviewId);
}
