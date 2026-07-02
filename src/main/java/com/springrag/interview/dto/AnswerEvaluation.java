package com.springrag.interview.dto;

public class AnswerEvaluation {

    private Double score;

    private String strengths;

    private String weaknesses;

    private String improvementSuggestions;

    private Integer technicalAccuracy;

    private Integer completeness;

    private Integer practicalUnderstanding;

    private Integer communicationClarity;

    public AnswerEvaluation() {
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getStrengths() {
        return strengths;
    }

    public void setStrengths(String strengths) {
        this.strengths = strengths;
    }

    public String getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(String weaknesses) {
        this.weaknesses = weaknesses;
    }

    public String getImprovementSuggestions() {
        return improvementSuggestions;
    }

    public void setImprovementSuggestions(String improvementSuggestions) {
        this.improvementSuggestions = improvementSuggestions;
    }

    public Integer getTechnicalAccuracy() {
        return technicalAccuracy;
    }

    public void setTechnicalAccuracy(Integer technicalAccuracy) {
        this.technicalAccuracy = technicalAccuracy;
    }

    public Integer getCompleteness() {
        return completeness;
    }

    public void setCompleteness(Integer completeness) {
        this.completeness = completeness;
    }

    public Integer getPracticalUnderstanding() {
        return practicalUnderstanding;
    }

    public void setPracticalUnderstanding(Integer practicalUnderstanding) {
        this.practicalUnderstanding = practicalUnderstanding;
    }

    public Integer getCommunicationClarity() {
        return communicationClarity;
    }

    public void setCommunicationClarity(Integer communicationClarity) {
        this.communicationClarity = communicationClarity;
    }
}