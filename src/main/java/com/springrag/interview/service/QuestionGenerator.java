package com.springrag.interview.service;

import com.springrag.interview.domain.enums.DifficultyLevel;
import com.springrag.interview.domain.enums.InterviewType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Single place that turns (type, difficulty, prior context) into ONE interview question via the LLM.
 * Shared by per-interview plan generation, adaptive follow-ups, and the admin question bank — so the
 * prompt and behavior live in exactly one location instead of being copied per caller.
 */
@Component
public class QuestionGenerator {

    private final ChatClient chatClient;

    public QuestionGenerator(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * Generate one question for the given stack/difficulty, avoiding repetition of {@code priorQuestions}.
     * {@code priorAnswers} (optional, aligned by index) lets adaptive follow-ups probe deeper.
     */
    public String generateOne(InterviewType type, DifficultyLevel difficulty,
                              List<String> priorQuestions, List<String> priorAnswers) {
        String text = chatClient.prompt(buildPrompt(type, difficulty, priorQuestions, priorAnswers))
                .call().content();
        return text != null ? text.strip() : "";
    }

    private String buildPrompt(InterviewType type, DifficultyLevel difficulty,
                               List<String> questions, List<String> answers) {
        StringBuilder prompt = new StringBuilder("""
                You are an experienced technical interviewer.

                Generate ONLY ONE interview question.
                Do not provide explanations or answers. Return only the question.

                Type:
                """)
                .append(type)
                .append("\n\nDifficulty:\n")
                .append(difficulty)
                .append("\n\n");

        if (questions == null || questions.isEmpty()) {
            return prompt.append("This is the first question. Start the interview.\n").toString();
        }

        prompt.append("Questions already in this interview (do not repeat them):\n\n");
        for (int i = 0; i < questions.size(); i++) {
            prompt.append("Question ").append(i + 1).append(": ").append(questions.get(i)).append("\n");
            if (answers != null && i < answers.size()) {
                prompt.append("Answer: ").append(answers.get(i)).append("\n");
            }
            prompt.append("\n");
        }
        return prompt.append("Ask the next question.\n").toString();
    }
}
