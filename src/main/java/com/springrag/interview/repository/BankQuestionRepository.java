package com.springrag.interview.repository;

import com.springrag.interview.domain.BankQuestion;
import com.springrag.interview.domain.enums.DifficultyLevel;
import com.springrag.interview.domain.enums.InterviewType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BankQuestionRepository extends JpaRepository<BankQuestion, UUID> {

    List<BankQuestion> findByTypeAndDifficulty(InterviewType type, DifficultyLevel difficulty);

    List<BankQuestion> findAllByOrderByCreatedAtDesc();
}
