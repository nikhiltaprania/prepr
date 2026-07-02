package com.springrag.chat.service.impl;

import com.springrag.chat.service.PromptService;
import com.springrag.conversation.domain.Message;
import com.springrag.conversation.domain.MessageRole;
import com.springrag.memory.dto.ConversationContext;
import com.springrag.retrieval.dto.SearchResultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the prompt for the conversational chat assistant. Memory-first: a system message carries the
 * conversation summary and the user's long-term memories, followed by the recent conversation turns.
 * No knowledge-base context is injected here (that belongs to the Knowledge & RAG module).
 */
@Service
public class PromptServiceImpl implements PromptService {

    private static final Logger log = LoggerFactory.getLogger(PromptServiceImpl.class);

    @Override
    public Prompt buildPrompt(ConversationContext context) {
        StringBuilder systemPrompt = new StringBuilder("""
                You are a helpful, friendly AI assistant having an ongoing conversation with a user.

                Follow these rules:
                - Be natural, conversational, accurate, and concise.
                - Use the conversation history and the remembered facts below to stay consistent and
                  personalize your replies.
                - Use follow-up references ("that", "he", "earlier") in light of the conversation so far.
                - Never invent facts about the user; if you don't know something, say so or ask.
                """);

        if (context.getSummary() != null && !context.getSummary().isBlank()) {
            systemPrompt.append("\nConversation Summary:\n\n").append(context.getSummary()).append("\n");
        }

        List<SearchResultResponse> memories = context.getMemoryResults();
        if (memories != null && !memories.isEmpty()) {
            systemPrompt.append("\nWhat you remember about the user:\n\n");
            memories.forEach(m -> systemPrompt.append("- ").append(m.getContent()).append("\n"));
            systemPrompt.append("\n");
        }

        List<org.springframework.ai.chat.messages.Message> promptMessages = new ArrayList<>();
        promptMessages.add(new SystemMessage(systemPrompt.toString()));

        for (Message message : context.getRecentMessages()) {
            if (message.getRole() == MessageRole.USER) {
                promptMessages.add(new UserMessage(message.getContent()));
            } else if (message.getRole() == MessageRole.ASSISTANT) {
                promptMessages.add(new AssistantMessage(message.getContent()));
            }
        }

        log.info("Chat prompt built. memories={}, recentMessages={}, systemPromptChars={}",
                memories != null ? memories.size() : 0, context.getRecentMessages().size(), systemPrompt.length());
        log.debug("System prompt sent to model:\n{}", systemPrompt);

        return new Prompt(promptMessages);
    }
}
