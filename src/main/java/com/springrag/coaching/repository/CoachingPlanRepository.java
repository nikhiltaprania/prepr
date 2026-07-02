package com.springrag.coaching.repository;

import com.springrag.coaching.domain.CoachingPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CoachingPlanRepository extends JpaRepository<CoachingPlan, UUID> {

    Optional<CoachingPlan> findByUserId(UUID userId);
}
