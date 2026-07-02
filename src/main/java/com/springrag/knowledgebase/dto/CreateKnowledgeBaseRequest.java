package com.springrag.knowledgebase.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateKnowledgeBaseRequest {

    @NotBlank
    private String name;

    private String description;

    public CreateKnowledgeBaseRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(
            String description) {

        this.description = description;
    }
}