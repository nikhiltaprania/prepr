package com.springrag.report.web;

import com.springrag.report.dto.InterviewReportResponse;
import com.springrag.report.service.InterviewReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/interview-reports")
public class InterviewReportController {

    private final InterviewReportService reportService;

    public InterviewReportController(InterviewReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/generate/{interviewId}")
    @PreAuthorize("@access.ownsInterview(#interviewId) or hasRole('ADMIN')")
    public InterviewReportResponse generate(@PathVariable UUID interviewId) {
        return reportService.generateReport(interviewId);
    }

    @GetMapping("/interview/{interviewId}")
    @PreAuthorize("@access.ownsInterview(#interviewId) or hasRole('ADMIN')")
    public InterviewReportResponse get(@PathVariable UUID interviewId) {
        return reportService.getReport(interviewId);
    }
}
