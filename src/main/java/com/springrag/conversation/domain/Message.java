package com.springrag.conversation.domain;

import jakarta.persistence.*;
import com.springrag.common.domain.BaseEntity;

@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_message_conversation_id", columnList = "conversation_id")
})
public class Message extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;
    @Column(columnDefinition = "TEXT")
    private String content;
    private Integer tokenCount;

    public Message() {
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public MessageRole getRole() {
        return role;
    }

    public void setRole(MessageRole role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }
}
