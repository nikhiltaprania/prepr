package com.springrag.coaching.dto;

public class CoachingPlanResult {

    private String focusAreas;

    private String recommendations;

    private String practicePlan;

    private String estimatedDuration;

    public CoachingPlanResult() {
    }

    public String getFocusAreas() {
        return focusAreas;
    }

    public void setFocusAreas(
            String focusAreas) {

        this.focusAreas =
                focusAreas;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(
            String recommendations) {

        this.recommendations =
                recommendations;
    }

    public String getPracticePlan() {
        return practicePlan;
    }

    public void setPracticePlan(
            String practicePlan) {

        this.practicePlan =
                practicePlan;
    }

    public String getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(
            String estimatedDuration) {

        this.estimatedDuration =
                estimatedDuration;
    }
}