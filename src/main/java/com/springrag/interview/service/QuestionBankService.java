package com.springrag.interview.service;

import com.springrag.interview.domain.BankQuestion;
import com.springrag.interview.domain.enums.DifficultyLevel;
import com.springrag.interview.domain.enums.InterviewType;
import com.springrag.interview.dto.BankQuestionRequest;
import com.springrag.interview.dto.BankQuestionResponse;

import java.util.List;
import java.util.UUID;

/** Admin-managed reusable question bank; also seeds interview plans via {@link #draw}. */
public interface QuestionBankService {

    List<BankQuestionResponse> list();

    BankQuestionResponse create(BankQuestionRequest request);

    BankQuestionResponse update(UUID id, BankQuestionRequest request);

    void delete(UUID id);

    /** Generate {@code count} AI questions into the bank for a stack/difficulty/category. */
    List<BankQuestionResponse> generate(InterviewType type, DifficultyLevel difficulty, String category, int count);

    /** Up to {@code limit} random bank questions for a plan, excluding any already-used texts. */
    List<BankQuestion> draw(InterviewType type, DifficultyLevel difficulty, int limit, List<String> excludeTexts);
}
