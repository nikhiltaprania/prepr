package com.springrag.retrieval.service.impl;

import com.springrag.retrieval.dto.SearchResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetrievalServiceImplTest {

    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private RetrievalServiceImpl retrievalService;

    @Test
    void mapsDocumentsToSearchResultsAndUsesDocumentScore() {
        UUID knowledgeBaseId = UUID.randomUUID();
        Document document = Document.builder()
                .text("chunk text")
                .metadata(Map.of(
                        "documentId", "doc-1",
                        "fileName", "guide.pdf",
                        "chunkNumber", 3,
                        "knowledgeBaseId", knowledgeBaseId.toString()))
                .score(0.8)
                .build();

        when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(List.of(document));

        SearchResponse response = retrievalService.search(knowledgeBaseId, "how to?");

        assertThat(response.getQuery()).isEqualTo("how to?");
        assertThat(response.getResultCount()).isEqualTo(1);
        assertThat(response.getResults()).hasSize(1);
        assertThat(response.getResults().getFirst().getContent()).isEqualTo("chunk text");
        assertThat(response.getResults().getFirst().getFileName()).isEqualTo("guide.pdf");
        assertThat(response.getResults().getFirst().getChunkNumber()).isEqualTo(3);
        assertThat(response.getResults().getFirst().getScore()).isEqualTo(0.8);
    }

    @Test
    void returnsEmptyResponseWhenNoMatches() {
        when(vectorStore.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(List.of());

        SearchResponse response = retrievalService.search(UUID.randomUUID(), "nothing");

        assertThat(response.getResultCount()).isZero();
        assertThat(response.getResults()).isEmpty();
    }
}
