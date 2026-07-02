package com.springrag.interview.service.impl;

import com.springrag.common.exception.ResourceNotFoundException;
import com.springrag.interview.domain.BankQuestion;
import com.springrag.interview.domain.InterviewAnswer;
import com.springrag.interview.domain.InterviewQuestion;
import com.springrag.interview.domain.InterviewSession;
import com.springrag.interview.dto.CreateInterviewQuestionRequest;
import com.springrag.interview.dto.InterviewQuestionResponse;
import com.springrag.interview.repository.InterviewAnswerRepository;
import com.springrag.interview.repository.InterviewQuestionRepository;
import com.springrag.interview.repository.InterviewSessionRepository;
import com.springrag.interview.service.InterviewQuestionService;
import com.springrag.interview.service.QuestionBankService;
import com.springrag.interview.service.QuestionGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns the interview's question plan and its execution order — the single source of truth for
 * "which question was asked last and which is next". Plans are seeded from the curated question bank
 * first ({@link QuestionBankService#draw}), falling back to LLM generation ({@link QuestionGenerator})
 * for any shortfall, so admin-managed questions flow into real interviews. Adaptive follow-ups are
 * generated only when the session is flagged adaptive and the plan is exhausted.
 */
@Service
public class InterviewQuestionServiceImpl implements InterviewQuestionService {

    private static final Logger log = LoggerFactory.getLogger(InterviewQuestionServiceImpl.class);

    private final InterviewQuestionRepository questionRepository;
    private final InterviewSessionRepository interviewRepository;
    private final InterviewAnswerRepository answerRepository;
    private final QuestionGenerator questionGenerator;
    private final QuestionBankService questionBankService;

    public InterviewQuestionServiceImpl(InterviewQuestionRepository questionRepository,
                                        InterviewSessionRepository interviewRepository,
                                        InterviewAnswerRepository answerRepository,
                                        QuestionGenerator questionGenerator,
                                        QuestionBankService questionBankService) {
        this.questionRepository = questionRepository;
        this.interviewRepository = interviewRepository;
        this.answerRepository = answerRepository;
        this.questionGenerator = questionGenerator;
        this.questionBankService = questionBankService;
    }

    @Override
    @Transactional
    public InterviewQuestionResponse create(CreateInterviewQuestionRequest request) {
        InterviewSession interview = interviewRepository.findById(request.getInterviewId())
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + request.getInterviewId()));

        InterviewQuestion question = new InterviewQuestion();
        question.setInterview(interview);
        question.setSequenceNumber(request.getSequenceNumber() != null
                ? request.getSequenceNumber() : nextSequence(request.getInterviewId()));
        question.setQuestion(request.getQuestion());
        question.setAiGenerated(false);

        InterviewQuestion saved = questionRepository.save(question);
        log.info("Manual question added to plan. interviewId={}, seq={}", interview.getId(), saved.getSequenceNumber());
        return toResponse(saved);
    }

    @Override
    public List<InterviewQuestionResponse> generatePlan(UUID interviewId, int count) {
        InterviewSession interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + interviewId));

        List<InterviewQuestion> planned = questionRepository.findByInterviewIdOrderBySequenceNumberAsc(interviewId);
        int sequence = planned.isEmpty() ? 0 : planned.get(planned.size() - 1).getSequenceNumber();
        List<String> plannedTexts = new ArrayList<>(planned.stream().map(InterviewQuestion::getQuestion).toList());

        // Curated bank questions first; LLM generation fills the remainder.
        List<BankQuestion> drawn = questionBankService.draw(interview.getType(), interview.getDifficulty(), count, plannedTexts);

        List<InterviewQuestionResponse> created = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String text;
            boolean aiGenerated;
            if (i < drawn.size()) {
                BankQuestion b = drawn.get(i);
                text = b.getQuestion();
                aiGenerated = b.isAiGenerated();
            } else {
                text = questionGenerator.generateOne(interview.getType(), interview.getDifficulty(), plannedTexts, List.of());
                aiGenerated = true;
            }
            if (text == null || text.isBlank()) continue;

            InterviewQuestion question = new InterviewQuestion();
            question.setInterview(interview);
            question.setSequenceNumber(++sequence);
            question.setQuestion(text.strip());
            question.setAiGenerated(aiGenerated);
            questionRepository.save(question);
            plannedTexts.add(text);
            created.add(toResponse(question));
        }
        log.info("Generated plan. interviewId={}, count={}, fromBank={}", interviewId, created.size(), drawn.size());
        return created;
    }

    @Override
    public Optional<InterviewQuestionResponse> peekNextQuestion(UUID interviewId) {
        InterviewSession interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + interviewId));

        // 1. The next planned, not-yet-asked question, in sequence order.
        InterviewQuestion planned = questionRepository
                .findFirstByInterviewIdAndAskedAtIsNullOrderBySequenceNumberAsc(interviewId).orElse(null);
        if (planned != null) {
            return Optional.of(toResponse(planned));
        }

        // 2. Plan exhausted. Only an adaptive interview generates (and parks) a follow-up.
        if (!interview.isAdaptive()) {
            return Optional.empty();
        }

        List<InterviewQuestion> questions = questionRepository.findByInterviewIdOrderBySequenceNumberAsc(interviewId);
        List<InterviewAnswer> answers = answerRepository.findByQuestionInterviewId(interviewId);
        String text = questionGenerator.generateOne(interview.getType(), interview.getDifficulty(),
                questions.stream().map(InterviewQuestion::getQuestion).toList(),
                answers.stream().map(InterviewAnswer::getAnswer).toList());

        InterviewQuestion question = new InterviewQuestion();
        question.setInterview(interview);
        question.setSequenceNumber(nextSequence(interviewId));
        question.setQuestion(text != null ? text.strip() : "");
        question.setAiGenerated(true);
        InterviewQuestion parked = questionRepository.save(question);
        log.info("Adaptive follow-up generated (parked). interviewId={}, seq={}", interviewId, parked.getSequenceNumber());
        return Optional.of(toResponse(parked));
    }

    @Override
    @Transactional
    public InterviewQuestionResponse markAsked(UUID questionId) {
        InterviewQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));
        question.setAskedAt(LocalDateTime.now());
        return toResponse(questionRepository.save(question));
    }

    @Override
    public InterviewQuestionResponse nextQuestion(UUID interviewId) {
        return peekNextQuestion(interviewId)
                .map(question -> markAsked(question.getId()))
                .orElseGet(() -> InterviewQuestionResponse.completed(interviewId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewQuestionResponse> getInterviewQuestions(UUID interviewId) {
        return questionRepository.findByInterviewIdOrderBySequenceNumberAsc(interviewId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private int nextSequence(UUID interviewId) {
        return questionRepository.findFirstByInterviewIdOrderBySequenceNumberDesc(interviewId)
                .map(q -> q.getSequenceNumber() + 1)
                .orElse(1);
    }

    private InterviewQuestionResponse toResponse(InterviewQuestion question) {
        InterviewQuestionResponse response = new InterviewQuestionResponse();
        response.setId(question.getId());
        response.setInterviewId(question.getInterview().getId());
        response.setSequenceNumber(question.getSequenceNumber());
        response.setQuestion(question.getQuestion());
        response.setAiGenerated(question.getAiGenerated());
        return response;
    }
}
