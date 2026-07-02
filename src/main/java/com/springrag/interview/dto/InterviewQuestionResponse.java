package com.springrag.interview.dto;

import java.util.UUID;

public class InterviewQuestionResponse {

    private UUID id;

    private UUID interviewId;

    private Integer sequenceNumber;

    private String question;

    private Boolean aiGenerated;

    /** True when the interview has no more questions to ask (no question is returned in that case). */
    private boolean completed;

    public InterviewQuestionResponse() {
    }

    public static InterviewQuestionResponse completed(UUID interviewId) {
        InterviewQuestionResponse response = new InterviewQuestionResponse();
        response.interviewId = interviewId;
        response.completed = true;
        return response;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(UUID interviewId) {
        this.interviewId = interviewId;
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
}