package com.springrag.interview.domain;

import com.springrag.interview.domain.enums.DifficultyLevel;
import com.springrag.interview.domain.enums.InterviewStatus;
import com.springrag.interview.domain.enums.InterviewType;
import com.springrag.user.domain.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * An interview session for a candidate ({@link User}). Holds the lifecycle (status, timings,
 * overall score) and — once generated — the aggregated report (strengths, weaknesses,
 * recommendations, summary), which is a one-to-one outcome of the session.
 */
@Entity
@Table(name = "interview_sessions", indexes = {
        @Index(name = "idx_interview_session_user_id", columnList = "user_id")
})
public class InterviewSession {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private InterviewType type;

    @Enumerated(EnumType.STRING)
    private DifficultyLevel difficulty;

    @Enumerated(EnumType.STRING)
    private InterviewStatus status;

    /** When true, the interviewer may generate follow-up questions once the planned questions run out. */
    private boolean adaptive;

    /** The conversation backing this interview's turn-by-turn dialogue (reuses the memory module). */
    private UUID conversationId;

    private Double overallScore;

    @Column(columnDefinition = "TEXT")
    private String reportStrengths;

    @Column(columnDefinition = "TEXT")
    private String reportWeaknesses;

    @Column(columnDefinition = "TEXT")
    private String reportRecommendations;

    @Column(columnDefinition = "TEXT")
    private String reportSummary;

    private LocalDateTime reportGeneratedAt;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public InterviewSession() {
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = InterviewStatus.CREATED;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public InterviewType getType() {
        return type;
    }

    public void setType(InterviewType type) {
        this.type = type;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public InterviewStatus getStatus() {
        return status;
    }

    public void setStatus(InterviewStatus status) {
        this.status = status;
    }

    public boolean isAdaptive() {
        return adaptive;
    }

    public void setAdaptive(boolean adaptive) {
        this.adaptive = adaptive;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }

    public String getReportStrengths() {
        return reportStrengths;
    }

    public void setReportStrengths(String reportStrengths) {
        this.reportStrengths = reportStrengths;
    }

    public String getReportWeaknesses() {
        return reportWeaknesses;
    }

    public void setReportWeaknesses(String reportWeaknesses) {
        this.reportWeaknesses = reportWeaknesses;
    }

    public String getReportRecommendations() {
        return reportRecommendations;
    }

    public void setReportRecommendations(String reportRecommendations) {
        this.reportRecommendations = reportRecommendations;
    }

    public String getReportSummary() {
        return reportSummary;
    }

    public void setReportSummary(String reportSummary) {
        this.reportSummary = reportSummary;
    }

    public LocalDateTime getReportGeneratedAt() {
        return reportGeneratedAt;
    }

    public void setReportGeneratedAt(LocalDateTime reportGeneratedAt) {
        this.reportGeneratedAt = reportGeneratedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
