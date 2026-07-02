package com.springrag.chat.dto;

/**
 * A conversational assistant reply. No citations — the chat assistant answers from conversation
 * history and memory, not from retrieved documents.
 */
public class ChatResponse {

    private String response;

    public ChatResponse() {
    }

    public ChatResponse(String response) {
        this.response = response;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }
}
