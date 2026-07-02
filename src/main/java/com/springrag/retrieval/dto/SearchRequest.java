package com.springrag.retrieval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class SearchRequest {

    @NotNull
    private UUID knowledgeBaseId;

    @NotBlank
    private String query;

    public SearchRequest() {
    }

    public UUID getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(
            UUID knowledgeBaseId) {

        this.knowledgeBaseId = knowledgeBaseId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(
            String query) {

        this.query = query;
    }
}