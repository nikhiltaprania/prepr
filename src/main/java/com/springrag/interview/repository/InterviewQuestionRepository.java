package com.springrag.interview.repository;

import com.springrag.interview.domain.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, UUID> {

    List<InterviewQuestion> findByInterviewIdOrderBySequenceNumberAsc(UUID interviewId);

    /** Owner id of the interview this question belongs to — for ownership checks (no entity load). */
    @Query("select q.interview.user.id from InterviewQuestion q where q.id = :questionId")
    Optional<UUID> findOwnerId(@Param("questionId") UUID questionId);

    /** The next planned question that has not yet been asked (progression: which to ask next). */
    Optional<InterviewQuestion> findFirstByInterviewIdAndAskedAtIsNullOrderBySequenceNumberAsc(UUID interviewId);

    /** The most recently asked question (progression: which was asked last). */
    Optional<InterviewQuestion> findFirstByInterviewIdAndAskedAtIsNotNullOrderBySequenceNumberDesc(UUID interviewId);

    /** The highest existing sequence number, for appending new questions. */
    Optional<InterviewQuestion> findFirstByInterviewIdOrderBySequenceNumberDesc(UUID interviewId);
}
