package com.springrag.memory.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.springrag.config.MemoryProperties;
import com.springrag.memory.domain.MemoryCategory;
import com.springrag.memory.domain.SemanticMemory;
import com.springrag.memory.dto.MemoryStatisticsResponse;
import com.springrag.memory.repository.SemanticMemoryRepository;
import com.springrag.memory.service.SemanticMemoryService;
import com.springrag.retrieval.dto.SearchResultResponse;

@Service
@Transactional
public class SemanticMemoryServiceImpl
        implements SemanticMemoryService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    SemanticMemoryServiceImpl.class);

    private final SemanticMemoryRepository
            repository;

    private final VectorStore vectorStore;

    private final MemoryProperties memoryProperties;

    public SemanticMemoryServiceImpl(
            SemanticMemoryRepository repository,
            VectorStore vectorStore,
            MemoryProperties memoryProperties) {

        this.repository = repository;
        this.vectorStore = vectorStore;
        this.memoryProperties = memoryProperties;
    }

    @Override
    public void storeMemory(UUID userId,
            UUID conversationId,
            MemoryCategory category,
            String content) {

        if (memoryExists(userId, content)) {

            log.info(
                    "Skipping duplicate memory. conversationId={}, content={}",
                    conversationId,
                    content);

            return;
        }

        log.info(
                "Storing semantic memory. conversationId={}, category={}",
                conversationId,
                category);

        Document memoryDocument =
                new Document(
                        content,
                        Map.of(
                                "type",
                                "memory",

                                "userId",
                                userId.toString(),

                                "conversationId",
                                conversationId.toString(),

                                "category",
                                category.name()
                        )
                );

        vectorStore.add(
                List.of(
                        memoryDocument));

        SemanticMemory memory =
                new SemanticMemory();

        memory.setUserId(userId);

        memory.setConversationId(
                conversationId);

        memory.setCategory(
                category);

        memory.setContent(
                content);

        repository.save(
                memory);

        log.info(
                "New semantic memory stored. category={}, content={}",
                category,
                content);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchResultResponse>
    retrieveMemories( UUID userId,
            String query) {

        List<Document> memories =
                vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(query)
                                .topK(memoryProperties.getSemanticTopK())
                                .filterExpression(memoryFilter(userId))
                                .build());

        return memories.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean memoryExists(UUID userId,
            String content) {

        List<Document> results =
                vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(content)
                                .topK(1)
                                .filterExpression(memoryFilter(userId))
                                .build());

        if (results == null
                || results.isEmpty()) {

            return false;
        }

        Object distance =
                results.getFirst()
                        .getMetadata()
                        .get("distance");

        if (distance == null) {

            return false;
        }

        double similarity =
                1.0 -
                        Double.parseDouble(
                                distance.toString());

        return similarity >= memoryProperties.getDuplicateThreshold();
    }

    /**
     * Builds a metadata filter scoping a vector search to a single user's stored memories.
     * Replaces hand-concatenated filter strings with the Spring AI expression builder.
     */
    private Filter.Expression memoryFilter(UUID userId) {

        FilterExpressionBuilder builder = new FilterExpressionBuilder();

        return builder.and(
                builder.eq("type", "memory"),
                builder.eq("userId", userId.toString())).build();
    }

    @Override
    @Transactional(readOnly = true)
    public MemoryStatisticsResponse
    getStatistics(
            UUID userId) {

        MemoryStatisticsResponse response =
                new MemoryStatisticsResponse();

        response.setUserId(
                userId);

        response.setTotalMemories(
                repository.countByUserId(
                        userId));

        response.setPersonal(
                repository.countByUserIdAndCategory(
                        userId,
                        MemoryCategory.PERSONAL));

        response.setPreference(
                repository.countByUserIdAndCategory(
                        userId,
                        MemoryCategory.PREFERENCE));

        response.setFact(
                repository.countByUserIdAndCategory(
                        userId,
                        MemoryCategory.FACT));

        response.setProject(
                repository.countByUserIdAndCategory(
                        userId,
                        MemoryCategory.PROJECT));

        response.setGoal(
                repository.countByUserIdAndCategory(
                        userId,
                        MemoryCategory.GOAL));

        return response;
    }

    private SearchResultResponse toResponse(
            Document document) {

        SearchResultResponse response =
                new SearchResultResponse();

        response.setContent(
                document.getText());

        Object distance =
                document.getMetadata()
                        .get("distance");

        if (distance != null) {

            response.setScore(
                    1.0 -
                            Double.parseDouble(
                                    distance.toString()));
        }

        return response;
    }
}