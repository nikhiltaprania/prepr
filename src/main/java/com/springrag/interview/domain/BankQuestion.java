package com.springrag.interview.domain;

import com.springrag.common.domain.BaseEntity;
import com.springrag.interview.domain.enums.DifficultyLevel;
import com.springrag.interview.domain.enums.InterviewType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * A reusable interview question in the admin-managed question bank, organized by technology stack
 * ({@link InterviewType}), difficulty, and a free-form category. Distinct from {@link InterviewQuestion}
 * (which is a question asked within one interview session): the bank is a curated library that seeds
 * interview plans. Manual and AI-generated entries both live here, flagged by {@code aiGenerated}.
 */
@Entity
@Table(name = "bank_questions", indexes = {
        @Index(name = "idx_bank_question_type_difficulty", columnList = "type, difficulty")
})
public class BankQuestion extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InterviewType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DifficultyLevel difficulty;

    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    private boolean aiGenerated;

    public BankQuestion() {
    }

    public InterviewType getType() { return type; }
    public void setType(InterviewType type) { this.type = type; }

    public DifficultyLevel getDifficulty() { return difficulty; }
    public void setDifficulty(DifficultyLevel difficulty) { this.difficulty = difficulty; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public boolean isAiGenerated() { return aiGenerated; }
    public void setAiGenerated(boolean aiGenerated) { this.aiGenerated = aiGenerated; }
}
