package com.springrag.chat.service;

import com.springrag.chat.dto.ChatResponse;

import java.util.UUID;

public interface ChatService {

    /**
     * A conversational, memory-first chat turn (like a general AI assistant). Context comes from the
     * conversation history, summary, and the user's long-term memories — not from a knowledge base.
     * Returns the assistant's reply (no citations).
     */
    ChatResponse chat(UUID conversationId, String message);
}
