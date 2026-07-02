package com.springrag.resume.service;

import com.springrag.resume.dto.ResumeAnalysisResponse;

import java.util.UUID;

public interface ResumeAnalysisService {

    ResumeAnalysisResponse analyzeResume(UUID userId, String fileName, String contentType, byte[] content);

    ResumeAnalysisResponse getResumeAnalysis(UUID userId);
}
