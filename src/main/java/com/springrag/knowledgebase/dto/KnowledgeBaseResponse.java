package com.springrag.knowledgebase.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import com.springrag.knowledgebase.domain.KnowledgeBase;

public class KnowledgeBaseResponse {

    private UUID id;

    private String name;

    private String description;

    private boolean active;

    private LocalDateTime createdAt;

    public KnowledgeBaseResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public boolean isActive() {
        return active;
    }

    public void setActive(
            boolean active) {

        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(
            LocalDateTime createdAt) {

        this.createdAt = createdAt;
    }

    @Override
    public String toString() {

        return """
            KnowledgeBase:
            id=%s
            name=%s
            description=%s
            """
                .formatted(
                        id,
                        name,
                        description);
    }
}