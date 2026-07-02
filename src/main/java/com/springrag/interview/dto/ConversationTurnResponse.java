package com.springrag.interview.dto;

import java.util.UUID;

/**
 * The interviewer's natural reply to a candidate turn (spoken aloud by the browser for voice),
 * plus whether the interview has now concluded.
 */
public class ConversationTurnResponse {

    private UUID interviewId;
    private String reply;
    private boolean completed;

    public ConversationTurnResponse() {
    }

    public ConversationTurnResponse(UUID interviewId, String reply, boolean completed) {
        this.interviewId = interviewId;
        this.reply = reply;
        this.completed = completed;
    }

    public UUID getInterviewId() {
        return interviewId;
    }

    public void setInterviewId(UUID interviewId) {
        this.interviewId = interviewId;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
