package com.springrag.retrieval.service;

import com.springrag.retrieval.dto.AskResponse;

import java.util.UUID;

public interface KnowledgeQaService {

    /**
     * Answers a question grounded in chunks retrieved from the given knowledge base, synthesized
     * across the retrieved passages and returned with source citations. When {@code conversationId}
     * is provided, the conversation memory (recent turns + summary) is reused so follow-up questions
     * resolve correctly, and the new question/answer are persisted into that conversation. If nothing
     * relevant is retrieved, it reports that the answer was not found in the knowledge base.
     */
    AskResponse ask(UUID conversationId, UUID knowledgeBaseId, String question);
}
