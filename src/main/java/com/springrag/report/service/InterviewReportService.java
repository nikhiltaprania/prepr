package com.springrag.report.service;

import com.springrag.report.dto.InterviewReportResponse;

import java.util.UUID;

public interface InterviewReportService {

    InterviewReportResponse generateReport(
            UUID interviewId);

    InterviewReportResponse getReport(
            UUID interviewId);
}