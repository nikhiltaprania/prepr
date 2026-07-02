package com.springrag.interview.dto;

import com.springrag.interview.domain.enums.DifficultyLevel;
import com.springrag.interview.domain.enums.InterviewType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreateInterviewRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private InterviewType type;

    @NotNull
    private DifficultyLevel difficulty;

    private boolean adaptive;

    public CreateInterviewRequest() {
    }

    public boolean isAdaptive() {
        return adaptive;
    }

    public void setAdaptive(boolean adaptive) {
        this.adaptive = adaptive;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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
}
