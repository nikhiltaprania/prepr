package com.springrag.interview.web;

import com.springrag.interview.dto.ConversationTurnRequest;
import com.springrag.interview.dto.ConversationTurnResponse;
import com.springrag.interview.dto.TranscriptEntryResponse;
import com.springrag.interview.service.InterviewConversationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Voice/text conversation channel for an interview. The browser handles microphone capture, speech-to-text,
 * and text-to-speech (Web Speech API); these endpoints drive the shared interview execution.
 */
@RestController
@RequestMapping("/api/voice/interviews/{interviewId}")
@PreAuthorize("@access.ownsInterview(#interviewId) or hasRole('ADMIN')")
public class InterviewConversationController {

    private final InterviewConversationService conversationService;

    public InterviewConversationController(InterviewConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping("/respond")
    public ConversationTurnResponse respond(@PathVariable UUID interviewId, @RequestBody ConversationTurnRequest request) {
        return conversationService.respond(interviewId, request.getText());
    }

    @GetMapping("/transcript")
    public List<TranscriptEntryResponse> getTranscript(@PathVariable UUID interviewId) {
        return conversationService.getTranscript(interviewId);
    }
}
