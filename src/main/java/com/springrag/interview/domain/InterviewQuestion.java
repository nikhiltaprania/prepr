package com.springrag.interview.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interview_questions", indexes = {
        @Index(name = "idx_interview_question_interview_id", columnList = "interview_id")
})
public class InterviewQuestion {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "interview_id",
            nullable = false)
    private InterviewSession interview;

    @Column(nullable = false)
    private Integer sequenceNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    private Boolean aiGenerated;

    /** Set when the question is actually presented to the candidate. null = planned but not yet asked. */
    private LocalDateTime askedAt;

    private LocalDateTime createdAt;

    public InterviewQuestion() {
    }

    @PrePersist
    public void prePersist() {

        createdAt =
                LocalDateTime.now();

        if (aiGenerated == null) {

            aiGenerated = false;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public InterviewSession getInterview() {
        return interview;
    }

    public void setInterview(InterviewSession interview) {
        this.interview = interview;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Boolean getAiGenerated() {
        return aiGenerated;
    }

    public void setAiGenerated(Boolean aiGenerated) {
        this.aiGenerated = aiGenerated;
    }

    public LocalDateTime getAskedAt() {
        return askedAt;
    }

    public void setAskedAt(LocalDateTime askedAt) {
        this.askedAt = askedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}