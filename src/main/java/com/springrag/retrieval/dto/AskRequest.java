package com.springrag.retrieval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * A knowledge-grounded question against one knowledge base. Retrieval-first: a knowledge base is
 * required. {@code conversationId} is optional — when set, conversation memory is reused so follow-up
 * questions resolve naturally.
 */
public class AskRequest {

    @NotNull
    private UUID knowledgeBaseId;

    @NotBlank
    private String question;

    private UUID conversationId;

    public AskRequest() {
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public UUID getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(UUID knowledgeBaseId) {
        this.knowledgeBaseId = knowledgeBaseId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }
}
