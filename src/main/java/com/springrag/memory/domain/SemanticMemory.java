package com.springrag.memory.domain;

import jakarta.persistence.*;
import java.util.UUID;
import com.springrag.common.domain.BaseEntity;

@Entity
@Table(name = "semantic_memories", indexes = {
        @Index(name = "idx_semantic_memory_user_id", columnList = "userId")
})
public class SemanticMemory extends BaseEntity {

    private UUID conversationId;
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private MemoryCategory category;

    @Column(length = 2000)
    private String content;

    public SemanticMemory() {
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public MemoryCategory getCategory() {
        return category;
    }

    public void setCategory(MemoryCategory category) {
        this.category = category;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}