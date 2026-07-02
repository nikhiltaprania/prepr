package com.springrag.interview.dto;

import com.springrag.interview.domain.TranscriptSpeaker;

/**
 * One line of the interview conversation transcript (derived from the conversation messages).
 */
public class TranscriptEntryResponse {

    private TranscriptSpeaker speaker;
    private String content;

    public TranscriptEntryResponse() {
    }

    public TranscriptEntryResponse(TranscriptSpeaker speaker, String content) {
        this.speaker = speaker;
        this.content = content;
    }

    public TranscriptSpeaker getSpeaker() {
        return speaker;
    }

    public void setSpeaker(TranscriptSpeaker speaker) {
        this.speaker = speaker;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
