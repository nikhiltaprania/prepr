package com.springrag.memory.domain;

import jakarta.persistence.*;
import com.springrag.common.domain.BaseEntity;
import com.springrag.conversation.domain.Conversation;

@Entity
@Table(name = "conversation_summaries", indexes = {
        @Index(name = "idx_conversation_summary_conversation_id", columnList = "conversation_id", unique = true)
})
public class ConversationSummary extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false, updatable = false)
    private Conversation conversation;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;
    private Integer summarizedMessageCount = 0;

    public ConversationSummary() {
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getSummarizedMessageCount() {
        return summarizedMessageCount;
    }

    public void setSummarizedMessageCount(Integer summarizedMessageCount) {
        this.summarizedMessageCount = summarizedMessageCount;
    }
}
