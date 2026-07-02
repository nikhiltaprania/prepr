package com.springrag.memory.service;

import java.util.List;
import java.util.UUID;
import com.springrag.memory.domain.MemoryCategory;
import com.springrag.memory.dto.MemoryStatisticsResponse;
import com.springrag.retrieval.dto.SearchResultResponse;

public interface SemanticMemoryService {

    void storeMemory(
            UUID userId,
            UUID conversationId,
            MemoryCategory category,
            String content);

    List<SearchResultResponse> retrieveMemories(
            UUID userId,
            String query);

    boolean memoryExists(
            UUID userId,
            String content);

    MemoryStatisticsResponse
    getStatistics(
            UUID userId);
}