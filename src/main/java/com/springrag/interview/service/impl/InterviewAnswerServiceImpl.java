package com.springrag.interview.service.impl;

import com.springrag.common.exception.ResourceNotFoundException;
import com.springrag.common.exception.DuplicateResourceException;

import com.springrag.interview.domain.InterviewAnswer;
import com.springrag.interview.domain.InterviewQuestion;
import com.springrag.interview.dto.CreateInterviewAnswerRequest;
import com.springrag.interview.dto.InterviewAnswerResponse;
import com.springrag.interview.evaluation.InterviewEvaluationService;
import com.springrag.interview.repository.InterviewAnswerRepository;
import com.springrag.interview.repository.InterviewQuestionRepository;

import com.springrag.interview.service.InterviewAnswerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InterviewAnswerServiceImpl
        implements InterviewAnswerService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    InterviewAnswerServiceImpl.class);

    private final InterviewAnswerRepository
            answerRepository;

    private final InterviewQuestionRepository
            questionRepository;

    private final InterviewEvaluationService interviewEvaluationService;

    public InterviewAnswerServiceImpl(InterviewAnswerRepository answerRepository, InterviewQuestionRepository questionRepository, InterviewEvaluationService interviewEvaluationService) {
        this.answerRepository = answerRepository;
        this.questionRepository = questionRepository;
        this.interviewEvaluationService = interviewEvaluationService;
    }

    @Override
    public InterviewAnswerResponse create(
            CreateInterviewAnswerRequest request) {

        if (answerRepository.existsByQuestionId(
                request.getQuestionId())) {

            throw new DuplicateResourceException(
                    "Answer already exists for question: "
                            + request.getQuestionId());
        }

        log.info(
                "Creating answer for questionId={}",
                request.getQuestionId());

        InterviewQuestion question =
                questionRepository.findById(
                                request.getQuestionId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Question not found: "
                                                + request.getQuestionId()));

        InterviewAnswer answer =
                new InterviewAnswer();

        answer.setQuestion(
                question);

        answer.setAnswer(
                request.getAnswer());

        InterviewAnswer savedAnswer =
                answerRepository.save(
                        answer);

        // Evaluation is fire-and-forget on a background executor: the (multi-second) LLM scoring must
        // not block the interview turn. The answer is already committed, so the async task can read it.
        // Scores/strengths/weaknesses are filled in shortly after; this response returns them unset.
        interviewEvaluationService
                .evaluateAnswer(
                        savedAnswer.getId());

        log.info(
                "Answer created; evaluation dispatched asynchronously. id={}",
                savedAnswer.getId());

        return toResponse(
                savedAnswer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InterviewAnswerResponse>
    getInterviewAnswers(
            UUID interviewId) {

        return answerRepository
                .findByQuestionInterviewId(
                        interviewId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private InterviewAnswerResponse toResponse(
            InterviewAnswer answer) {

        InterviewAnswerResponse response =
                new InterviewAnswerResponse();

        response.setId(
                answer.getId());

        response.setQuestionId(
                answer.getQuestion()
                        .getId());

        response.setAnswer(
                answer.getAnswer());

        response.setScore(
                answer.getScore());

        response.setStrengths(
                answer.getStrengths());

        response.setWeaknesses(
                answer.getWeaknesses());

        response.setImprovementSuggestions(
                answer.getImprovementSuggestions());

        return response;
    }
}