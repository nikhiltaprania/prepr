package com.springrag.chat.service.impl;

import com.springrag.config.AsyncConfig;
import com.springrag.memory.dto.ExtractedMemory;
import com.springrag.memory.service.MemoryExtractionService;
import com.springrag.memory.service.SemanticMemoryService;
import com.springrag.memory.service.SummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Off-request work that follows a chat turn: refresh the conversation summary and extract/store
 * long-term semantic memories. None of this affects the response the user already received, and each
 * step is one or more LLM/vector calls — so it runs on the shared AI executor instead of blocking the
 * request thread. The user message and assistant reply are already committed before this runs.
 */
@Component
public class ChatPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(ChatPostProcessor.class);

    private final SummaryService summaryService;
    private final MemoryExtractionService memoryExtractionService;
    private final SemanticMemoryService semanticMemoryService;

    public ChatPostProcessor(SummaryService summaryService,
                             MemoryExtractionService memoryExtractionService,
                             SemanticMemoryService semanticMemoryService) {
        this.summaryService = summaryService;
        this.memoryExtractionService = memoryExtractionService;
        this.semanticMemoryService = semanticMemoryService;
    }

    @Async(AsyncConfig.AI_EXECUTOR)
    public void process(UUID userId, UUID conversationId, String userMessage) {
        try {
            summaryService.updateSummary(conversationId);

            List<ExtractedMemory> memories = memoryExtractionService.extractMemories(userMessage);
            for (ExtractedMemory memory : memories) {
                semanticMemoryService.storeMemory(userId, conversationId, memory.getCategory(), memory.getContent());
            }
            log.debug("Chat post-processing done. conversationId={}, memoriesExtracted={}",
                    conversationId, memories.size());
        } catch (Exception ex) {
            log.error("Chat post-processing failed. conversationId={}", conversationId, ex);
        }
    }
}
