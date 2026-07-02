package com.springrag.interview.web;

import com.springrag.interview.dto.CreateInterviewAnswerRequest;
import com.springrag.interview.dto.InterviewAnswerResponse;
import com.springrag.interview.service.InterviewAnswerService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/interview-answers")
public class InterviewAnswerController {

    private final InterviewAnswerService answerService;

    public InterviewAnswerController(InterviewAnswerService answerService) {
        this.answerService = answerService;
    }

    @PostMapping
    @PreAuthorize("@access.ownsQuestion(#request.questionId) or hasRole('ADMIN')")
    public InterviewAnswerResponse create(@Valid @RequestBody CreateInterviewAnswerRequest request) {
        return answerService.create(request);
    }

    @GetMapping("/interview/{interviewId}")
    @PreAuthorize("@access.ownsInterview(#interviewId) or hasRole('ADMIN')")
    public List<InterviewAnswerResponse> getAnswers(@PathVariable UUID interviewId) {
        return answerService.getInterviewAnswers(interviewId);
    }
}
