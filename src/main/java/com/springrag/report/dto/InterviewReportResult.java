package com.springrag.report.dto;

public class InterviewReportResult {

    private Double overallScore;

    private String strengths;

    private String weaknesses;

    private String recommendations;

    private String summary;

    public InterviewReportResult() {
    }

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(
            Double overallScore) {

        this.overallScore =
                overallScore;
    }

    public String getStrengths() {
        return strengths;
    }

    public void setStrengths(
            String strengths) {

        this.strengths =
                strengths;
    }

    public String getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(
            String weaknesses) {

        this.weaknesses =
                weaknesses;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(
            String recommendations) {

        this.recommendations =
                recommendations;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(
            String summary) {

        this.summary =
                summary;
    }
}