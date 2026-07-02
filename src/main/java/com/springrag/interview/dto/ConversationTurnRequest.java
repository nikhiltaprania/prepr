package com.springrag.interview.dto;

/**
 * One candidate turn in a voice/text interview conversation — the candidate's answer to the current
 * question, already transcribed to text (in the browser, for voice).
 */
public class ConversationTurnRequest {

    private String text;

    public ConversationTurnRequest() {
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
