package com.springrag.memory.web;

import com.springrag.memory.service.SummaryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
public class SummaryController {

    private final SummaryService summaryService;

    public SummaryController(SummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @GetMapping("/{conversationId}/summary")
    @PreAuthorize("@access.ownsConversation(#conversationId) or hasRole('ADMIN')")
    public String getSummary(@PathVariable UUID conversationId) {
        return summaryService.getSummary(conversationId);
    }

    @PostMapping("/{conversationId}/summary/regenerate")
    @PreAuthorize("@access.ownsConversation(#conversationId) or hasRole('ADMIN')")
    public void regenerate(@PathVariable UUID conversationId) {
        summaryService.regenerateSummary(conversationId);
    }
}
