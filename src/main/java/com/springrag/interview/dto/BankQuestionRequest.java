package com.springrag.interview.dto;

import com.springrag.interview.domain.enums.DifficultyLevel;
import com.springrag.interview.domain.enums.InterviewType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Create/update payload for a bank question. */
public class BankQuestionRequest {

    @NotNull
    private InterviewType type;

    @NotNull
    private DifficultyLevel difficulty;

    private String category;

    @NotBlank
    private String question;

    public InterviewType getType() { return type; }
    public void setType(InterviewType type) { this.type = type; }

    public DifficultyLevel getDifficulty() { return difficulty; }
    public void setDifficulty(DifficultyLevel difficulty) { this.difficulty = difficulty; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
}
