package com.springrag.interview.service;

import com.springrag.interview.dto.ConversationTurnResponse;
import com.springrag.interview.dto.TranscriptEntryResponse;

import java.util.List;
import java.util.UUID;

/**
 * Drives an interview as a turn-based conversation — the channel used by the voice UI (browser
 * Web Speech) and by any text chat client. It reuses the shared interview execution: a turn records
 * the candidate's answer to the current question (with evaluation) and advances to the next question.
 */
public interface InterviewConversationService {

    /**
     * Processes one candidate turn: records {@code candidateText} as the answer to the current
     * question (if one is awaiting an answer), then returns the next question to ask, or a completion
     * signal if the interview is finished.
     */
    ConversationTurnResponse respond(UUID interviewId, String candidateText);

    /** The interview transcript, derived from the conversation messages. */
    List<TranscriptEntryResponse> getTranscript(UUID interviewId);
}
