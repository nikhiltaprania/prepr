package com.springrag.skill.dto;

import com.springrag.skill.domain.SkillCategory;

public class UserSkillResponse {

    private SkillCategory skillCategory;
    private Double averageScore;
    private Integer assessmentCount;

    public UserSkillResponse() {
    }

    public SkillCategory getSkillCategory() {
        return skillCategory;
    }

    public void setSkillCategory(SkillCategory skillCategory) {
        this.skillCategory = skillCategory;
    }

    public Double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(Double averageScore) {
        this.averageScore = averageScore;
    }

    public Integer getAssessmentCount() {
        return assessmentCount;
    }

    public void setAssessmentCount(Integer assessmentCount) {
        this.assessmentCount = assessmentCount;
    }
}
