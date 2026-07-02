package com.springrag.coaching.domain;

import com.springrag.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

/**
 * A candidate's single active coaching plan. One row per user (unique {@code user_id}) — regenerating
 * replaces the fields in place, so the user always has exactly one persisted, current plan that
 * survives refreshes and sessions.
 */
@Entity
@Table(name = "coaching_plans", uniqueConstraints = @UniqueConstraint(columnNames = "user_id"))
public class CoachingPlan extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(columnDefinition = "TEXT")
    private String focusAreas;

    @Column(columnDefinition = "TEXT")
    private String recommendations;

    @Column(columnDefinition = "TEXT")
    private String practicePlan;

    private String estimatedDuration;

    public CoachingPlan() {
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getFocusAreas() { return focusAreas; }
    public void setFocusAreas(String focusAreas) { this.focusAreas = focusAreas; }

    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }

    public String getPracticePlan() { return practicePlan; }
    public void setPracticePlan(String practicePlan) { this.practicePlan = practicePlan; }

    public String getEstimatedDuration() { return estimatedDuration; }
    public void setEstimatedDuration(String estimatedDuration) { this.estimatedDuration = estimatedDuration; }
}
