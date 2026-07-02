package com.springrag.interview.repository;

import com.springrag.interview.domain.InterviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, UUID> {

    List<InterviewAnswer> findByQuestionInterviewId(UUID interviewId);

    boolean existsByQuestionId(UUID questionId);

    /**
     * Loads an answer with its question and parent interview eagerly, so evaluation can build
     * its prompt without an open session (the LLM call must not hold a DB transaction).
     */
    @Query("select a from InterviewAnswer a join fetch a.question q join fetch q.interview where a.id = :id")
    Optional<InterviewAnswer> findWithQuestionAndInterviewById(@Param("id") UUID id);
}
