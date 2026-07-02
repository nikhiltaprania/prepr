package com.springrag.memory.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;
import com.springrag.common.ai.ChatOptionsPresets;
import com.springrag.memory.dto.ExtractedMemory;
import com.springrag.memory.service.MemoryExtractionService;

/**
 * Extracts durable, categorized memories from a user message using the LLM.
 * Uses Spring AI structured output ({@code .entity(...)}) which appends the
 * required response-format instructions and binds the result directly to POJOs,
 * replacing the previous hand-rolled JSON prompt + Jackson parsing.
 */
@Service
public class LlmMemoryExtractionServiceImpl implements MemoryExtractionService {

    private static final Logger log = LoggerFactory.getLogger(LlmMemoryExtractionServiceImpl.class);

    private static final ParameterizedTypeReference<List<ExtractedMemory>> MEMORY_LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final ChatClient chatClient;

    public LlmMemoryExtractionServiceImpl(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public List<ExtractedMemory> extractMemories(String message) {

        try {
            String instructions = """
                    You are a memory extraction engine.
                    Extract ONLY durable information worth remembering long-term.
                    Assign each item one category: PERSONAL, PREFERENCE, FACT, PROJECT, or GOAL.
                    If nothing is worth remembering, return an empty list.

                    Message:
                    %s
                    """.formatted(message);

            List<ExtractedMemory> memories = chatClient.prompt()
                    .user(instructions)
                    .options(ChatOptionsPresets.deterministic())
                    .call()
                    .entity(MEMORY_LIST_TYPE);

            return memories != null ? memories : Collections.emptyList();

        } catch (Exception ex) {
            log.warn("Memory extraction failed; skipping memory capture for this message", ex);
            return Collections.emptyList();
        }
    }
}
