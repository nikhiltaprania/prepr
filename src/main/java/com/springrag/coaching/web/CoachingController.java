package com.springrag.coaching.web;

import com.springrag.coaching.dto.CoachingPlanResponse;
import com.springrag.coaching.service.CoachingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/coaching")
public class CoachingController {

    private static final Logger log = LoggerFactory.getLogger(CoachingController.class);

    private final CoachingService coachingService;

    public CoachingController(CoachingService coachingService) {
        this.coachingService = coachingService;
    }

    /** Returns the persisted active plan (no generation). 404 if none exists yet. */
    @GetMapping("/users/{userId}/plan")
    @PreAuthorize("@access.isCurrentUser(#userId) or hasRole('ADMIN')")
    public CoachingPlanResponse getPlan(@PathVariable UUID userId) {
        return coachingService.getActivePlan(userId);
    }

    /** Generates a fresh, data-grounded plan and replaces the user's active plan. */
    @PostMapping("/users/{userId}/plan")
    @PreAuthorize("@access.isCurrentUser(#userId) or hasRole('ADMIN')")
    public CoachingPlanResponse generatePlan(@PathVariable UUID userId) {
        log.info("Generate coaching plan request. userId={}", userId);
        return coachingService.generatePlan(userId);
    }
}
