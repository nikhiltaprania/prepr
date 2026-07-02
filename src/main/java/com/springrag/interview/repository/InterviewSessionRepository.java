package com.springrag.interview.repository;

import com.springrag.interview.domain.InterviewSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {

    List<InterviewSession> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Owner id only — used for ownership checks without loading the entity or its lazy User. */
    @Query("select i.user.id from InterviewSession i where i.id = :interviewId")
    Optional<UUID> findOwnerId(@Param("interviewId") UUID interviewId);
}
