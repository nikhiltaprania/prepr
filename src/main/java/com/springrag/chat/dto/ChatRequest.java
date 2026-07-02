package com.springrag.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * A conversational chat turn. Memory-first: it needs only the conversation it belongs to — there is
 * no knowledge base and no document retrieval (use {@code /api/ask} for knowledge-grounded answers).
 */
public class ChatRequest {

    @NotNull
    private UUID conversationId;

    @NotBlank
    private String message;

    public ChatRequest() {
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
