package com.springrag.interview.web;

import com.springrag.interview.dto.CreateInterviewRequest;
import com.springrag.interview.dto.InterviewResponse;
import com.springrag.interview.service.InterviewService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private static final Logger log = LoggerFactory.getLogger(InterviewController.class);

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@access.isCurrentUser(#request.userId) or hasRole('ADMIN')")
    public InterviewResponse createInterview(@Valid @RequestBody CreateInterviewRequest request) {
        log.info("Create interview request received");
        return interviewService.create(request);
    }

    @PostMapping("/{interviewId}/start")
    @PreAuthorize("@access.ownsInterview(#interviewId) or hasRole('ADMIN')")
    public InterviewResponse startInterview(@PathVariable UUID interviewId) {
        log.info("Start interview request. id={}", interviewId);
        return interviewService.start(interviewId);
    }

    @GetMapping("/{interviewId}")
    @PreAuthorize("@access.ownsInterview(#interviewId) or hasRole('ADMIN')")
    public InterviewResponse getInterview(@PathVariable UUID interviewId) {
        return interviewService.getById(interviewId);
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@access.isCurrentUser(#userId) or hasRole('ADMIN')")
    public List<InterviewResponse> getUserInterviews(@PathVariable UUID userId) {
        return interviewService.getUserInterviews(userId);
    }
}
