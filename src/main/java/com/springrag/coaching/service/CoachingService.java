package com.springrag.coaching.service;

import com.springrag.coaching.dto.CoachingPlanResponse;

import java.util.UUID;

public interface CoachingService {

    /** The user's persisted active plan. Throws ResourceNotFoundException if none generated yet. */
    CoachingPlanResponse getActivePlan(UUID userId);

    /** Generate a fresh, data-grounded plan and persist it as the user's single active plan (replace). */
    CoachingPlanResponse generatePlan(UUID userId);
}
