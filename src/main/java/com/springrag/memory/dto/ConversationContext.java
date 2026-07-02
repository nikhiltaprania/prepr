package com.springrag.memory.dto;

import java.util.List;
import com.springrag.conversation.domain.Message;
import com.springrag.retrieval.dto.SearchResultResponse;

public class ConversationContext {

    private String summary;

    private List<Message> recentMessages;
    private ContextSource contextSource;
    private List<SearchResultResponse> memoryResults;

    public ConversationContext() {
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<Message> getRecentMessages() {
        return recentMessages;
    }

    public void setRecentMessages(List<Message> recentMessages) {

        this.recentMessages = recentMessages;
    }

    public ContextSource getContextSource() {
        return contextSource;
    }

    public void setContextSource(ContextSource contextSource) {
        this.contextSource = contextSource;
    }

    public List<SearchResultResponse> getMemoryResults() {
        return memoryResults;
    }

    public void setMemoryResults(List<SearchResultResponse> memoryResults) {
        this.memoryResults = memoryResults;
    }
}