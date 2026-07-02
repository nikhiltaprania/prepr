package com.springrag.resume.web;

import com.springrag.resume.dto.ResumeAnalysisResponse;
import com.springrag.resume.service.ResumeAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private static final Logger log = LoggerFactory.getLogger(ResumeController.class);

    private final ResumeAnalysisService resumeAnalysisService;

    public ResumeController(ResumeAnalysisService resumeAnalysisService) {
        this.resumeAnalysisService = resumeAnalysisService;
    }

    @PostMapping(value = "/users/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@access.isCurrentUser(#userId) or hasRole('ADMIN')")
    public ResumeAnalysisResponse analyzeResume(@PathVariable UUID userId,
                                                @RequestPart("file") MultipartFile file) {
        log.info("Resume upload request received. userId={}", userId);
        String contentType = file.getContentType() != null
                ? file.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return resumeAnalysisService.analyzeResume(userId, file.getOriginalFilename(), contentType, readBytes(file));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("@access.isCurrentUser(#userId) or hasRole('ADMIN')")
    public ResumeAnalysisResponse getResumeAnalysis(@PathVariable UUID userId) {
        log.info("Get resume analysis request. userId={}", userId);
        return resumeAnalysisService.getResumeAnalysis(userId);
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read uploaded resume", ex);
        }
    }
}
