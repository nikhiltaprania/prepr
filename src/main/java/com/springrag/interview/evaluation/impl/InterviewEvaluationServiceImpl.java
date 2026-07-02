package com.springrag.interview.evaluation.impl;

import com.springrag.common.ai.ChatOptionsPresets;
import com.springrag.common.exception.ResourceNotFoundException;

import com.springrag.interview.domain.InterviewAnswer;
import com.springrag.interview.domain.InterviewQuestion;
import com.springrag.interview.dto.AnswerEvaluation;
import com.springrag.interview.evaluation.InterviewEvaluationService;
import com.springrag.interview.repository.InterviewAnswerRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.springrag.config.AsyncConfig;

import java.util.UUID;

/**
 * Evaluates an answer with the LLM. The model call deliberately runs OUTSIDE any database
 * transaction: the answer (with its question and interview) is loaded eagerly first, then the
 * LLM is called with no session held, then the scores are persisted in a short {@code save}.
 * This prevents holding a JPA connection/transaction open across the multi-second LLM call.
 */
@Service
public class InterviewEvaluationServiceImpl
        implements InterviewEvaluationService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    InterviewEvaluationServiceImpl.class);

    private final ChatClient chatClient;

    private final InterviewAnswerRepository
            answerRepository;

    public InterviewEvaluationServiceImpl(
            ChatClient.Builder chatClientBuilder,
            InterviewAnswerRepository answerRepository) {

        this.chatClient =
                chatClientBuilder.build();

        this.answerRepository =
                answerRepository;
    }

    @Override
    @Async(AsyncConfig.AI_EXECUTOR)
    public void evaluateAnswer(
            UUID answerId) {

        try {
            doEvaluate(answerId);
        } catch (Exception ex) {
            // Async: there's no caller to surface this to. Log with the answer id so an unscored
            // answer is diagnosable; the report step tolerates missing per-answer scores.
            log.error("Async answer evaluation failed. answerId={}", answerId, ex);
        }
    }

    private void doEvaluate(UUID answerId) {

        InterviewAnswer answer =
                answerRepository.findWithQuestionAndInterviewById(
                                answerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Answer not found: "
                                                + answerId));

        InterviewQuestion question = answer.getQuestion();

        log.debug("Evaluating answer. answerId={}", answerId);

        String prompt = """
        You are a senior %s interviewer.

        Evaluate the candidate's answer professionally and fairly.

        Question:
        %s

        Candidate Answer:
        %s

        Evaluation Rubric

        technicalAccuracy (0-4)
        - Is the answer technically correct?
        - Are the concepts explained accurately?
        - Are there any factual mistakes?

        completeness (0-2)
        - Does the answer cover the important points?
        - Are critical concepts missing?

        practicalUnderstanding (0-2)
        - Does the answer demonstrate real-world experience?
        - Are practical examples or implications discussed?

        communicationClarity (0-2)
        - Is the explanation clear and easy to understand?
        - Is the answer well structured?

        Scoring Rules

        - technicalAccuracy must be between 0 and 4
        - completeness must be between 0 and 2
        - practicalUnderstanding must be between 0 and 2
        - communicationClarity must be between 0 and 2

        score =
        technicalAccuracy +
        completeness +
        practicalUnderstanding +
        communicationClarity

        Final score must be between 0 and 10.

        Evaluation Guidelines

        - Reward correct technical concepts.
        - Reward practical examples.
        - Reward depth of understanding.
        - Penalize factual mistakes.
        - Penalize missing critical concepts.
        - Do not be overly harsh.
        - Do not be overly generous.
        - Be realistic as a professional interviewer.

        Return:
        technicalAccuracy
        completeness
        practicalUnderstanding
        communicationClarity
        score
        strengths
        weaknesses
        improvementSuggestions
        """
                .formatted(
                        question.getInterview()
                                .getType(),
                        question.getQuestion(),
                        answer.getAnswer());

        AnswerEvaluation evaluation =
                chatClient.prompt(prompt)
                        .options(ChatOptionsPresets.deterministic())
                        .call()
                        .entity(
                                AnswerEvaluation.class);

        log.debug("Evaluation result. answerId={}, score={}", answerId, evaluation.getScore());

        answer.setScore(
                evaluation.getScore());

        answer.setStrengths(
                evaluation.getStrengths());

        answer.setWeaknesses(
                evaluation.getWeaknesses());

        answer.setImprovementSuggestions(
                evaluation.getImprovementSuggestions());

        answerRepository.save(
                answer);

        log.info(
                "Answer evaluated. answerId={}, score={}",
                answerId,
                evaluation.getScore());
    }
}