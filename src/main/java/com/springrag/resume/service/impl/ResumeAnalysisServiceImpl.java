package com.springrag.resume.service.impl;

import com.springrag.common.exception.ResourceNotFoundException;
import com.springrag.common.io.NamedByteArrayResource;
import com.springrag.resume.domain.Resume;
import com.springrag.resume.dto.ResumeAnalysisResponse;
import com.springrag.resume.dto.ResumeAnalysisResult;
import com.springrag.resume.repository.ResumeRepository;
import com.springrag.resume.service.ResumeAnalysisService;
import com.springrag.user.domain.User;
import com.springrag.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Parses and analyzes a resume into the candidate's {@link Resume} (extracted text + AI analysis).
 * Tika extraction and the analysis LLM call run OUTSIDE any transaction (the user is loaded first);
 * the resume is then persisted in a short transaction.
 */
@Service
public class ResumeAnalysisServiceImpl implements ResumeAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ResumeAnalysisServiceImpl.class);

    private final ChatClient chatClient;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;

    public ResumeAnalysisServiceImpl(ChatClient.Builder chatClientBuilder,
                                     UserRepository userRepository,
                                     ResumeRepository resumeRepository) {
        this.chatClient = chatClientBuilder.build();
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
    }

    @Override
    public ResumeAnalysisResponse analyzeResume(UUID userId, String fileName, String contentType, byte[] content) {
        log.info("Analyzing resume for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Resource resource = new NamedByteArrayResource(content, fileName);
        String resumeText = new TikaDocumentReader(resource).get().stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        // Model call runs with no transaction held.
        ResumeAnalysisResult result = chatClient.prompt(buildPrompt(resumeText))
                .call()
                .entity(ResumeAnalysisResult.class);

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setFileName(fileName);
        resume.setContentType(contentType);
        resume.setExtractedText(resumeText);
        resume.setCandidateSummary(result.getCandidateSummary());
        resume.setSkills(result.getSkills());
        resume.setExperience(result.getExperience());
        resume.setProjects(result.getProjects());
        resume.setEducation(result.getEducation());
        resume.setStrengths(result.getStrengths());
        resume.setAnalyzedAt(LocalDateTime.now());

        resumeRepository.save(resume);
        log.info("Resume analysis completed. userId={}", userId);
        return toResponse(userId, resume);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeAnalysisResponse getResumeAnalysis(UUID userId) {
        Resume resume = resumeRepository.findFirstByUserIdOrderByUploadedAtDesc(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found for userId=" + userId));
        return toResponse(userId, resume);
    }

    private String buildPrompt(String resumeText) {
        return """
                You are an expert technical recruiter.

                Analyze the following resume.

                Extract:

                candidateSummary
                skills
                experience
                projects
                education
                strengths

                Resume:

                %s
                """.formatted(resumeText);
    }

    private ResumeAnalysisResponse toResponse(UUID userId, Resume resume) {
        ResumeAnalysisResponse response = new ResumeAnalysisResponse();
        response.setUserId(userId);
        response.setCandidateSummary(resume.getCandidateSummary());
        response.setSkills(resume.getSkills());
        response.setExperience(resume.getExperience());
        response.setProjects(resume.getProjects());
        response.setEducation(resume.getEducation());
        response.setStrengths(resume.getStrengths());
        return response;
    }
}
