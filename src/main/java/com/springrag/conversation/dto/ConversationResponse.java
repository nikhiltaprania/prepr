package com.springrag.conversation.dto;

import java.util.UUID;

public class ConversationResponse {
    private UUID conversationId;

    private String title;

    public ConversationResponse() {
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
