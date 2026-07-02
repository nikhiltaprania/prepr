package com.springrag.interview.web;

import com.springrag.interview.dto.CreateInterviewQuestionRequest;
import com.springrag.interview.dto.InterviewQuestionResponse;
import com.springrag.interview.service.InterviewQuestionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/interview-questions")
public class InterviewQuestionController {

    private final InterviewQuestionService questionService;

    public InterviewQuestionController(InterviewQuestionService questionService) {
        this.questionService = questionService;
    }

    /** Author a manual question (added to the plan at its sequence number). */
    @PostMapping
    @PreAuthorize("@access.ownsInterview(#request.interviewId) or hasRole('ADMIN')")
    public InterviewQuestionResponse create(@Valid @RequestBody CreateInterviewQuestionRequest request) {
        return questionService.create(request);
    }

    /** Generate {@code count} AI questions and append them to the plan (in order, not yet asked). */
    @PostMapping("/plan/{interviewId}")
    @PreAuthorize("@access.ownsInterview(#interviewId) or hasRole('ADMIN')")
    public List<InterviewQuestionResponse> generatePlan(@PathVariable UUID interviewId,
                                                        @RequestParam(defaultValue = "5") int count) {
        return questionService.generatePlan(interviewId, count);
    }

    /** Ask the next question in order (marks it asked). Reports completion when the plan is exhausted. */
    @PostMapping("/next/{interviewId}")
    @PreAuthorize("@access.ownsInterview(#interviewId) or hasRole('ADMIN')")
    public InterviewQuestionResponse next(@PathVariable UUID interviewId) {
        return questionService.nextQuestion(interviewId);
    }

    @GetMapping("/interview/{interviewId}")
    @PreAuthorize("@access.ownsInterview(#interviewId) or hasRole('ADMIN')")
    public List<InterviewQuestionResponse> getQuestions(@PathVariable UUID interviewId) {
        return questionService.getInterviewQuestions(interviewId);
    }
}
