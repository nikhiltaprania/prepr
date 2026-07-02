package com.springrag.memory.dto;

import java.util.List;
import com.springrag.conversation.dto.MessageResponse;

public class ContextResponse {
    private String source;

    private String summary;

    private List<MessageResponse> recentMessages;

    public ContextResponse() {
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<MessageResponse> getRecentMessages() {
        return recentMessages;
    }

    public void setRecentMessages(List<MessageResponse> recentMessages) {
        this.recentMessages = recentMessages;
    }
}
