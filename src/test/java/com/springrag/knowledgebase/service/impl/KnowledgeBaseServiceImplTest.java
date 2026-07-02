package com.springrag.knowledgebase.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.springrag.common.exception.ResourceNotFoundException;
import com.springrag.knowledgebase.domain.KnowledgeBase;
import com.springrag.knowledgebase.dto.KnowledgeBaseStatisticsResponse;
import com.springrag.knowledgebase.repository.DocumentChunkRepository;
import com.springrag.knowledgebase.repository.DocumentRepository;
import com.springrag.knowledgebase.repository.KnowledgeBaseRepository;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceImplTest {

    @Mock
    private KnowledgeBaseRepository repository;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentChunkRepository documentChunkRepository;
    @Mock
    private VectorStore vectorStore;

    @InjectMocks
    private KnowledgeBaseServiceImpl service;

    @Test
    void statisticsUseAggregateCountWithoutPerDocumentQueries() {
        UUID kbId = UUID.randomUUID();
        when(documentRepository.countDocumentEntitiesByKnowledgeBaseId(kbId)).thenReturn(3L);
        when(documentChunkRepository.countByKnowledgeBaseId(kbId)).thenReturn(10L);

        KnowledgeBaseStatisticsResponse stats = service.getStatistics(kbId);

        assertThat(stats.getDocumentCount()).isEqualTo(3L);
        assertThat(stats.getChunkCount()).isEqualTo(10L);
        // The previous N+1 implementation loaded every document to count chunks; ensure it no longer does.
        verify(documentRepository, never()).findByKnowledgeBaseId(any());
    }

    @Test
    void deleteRemovesEmbeddingsThenEntity() {
        UUID kbId = UUID.randomUUID();
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(kbId);
        when(repository.findById(kbId)).thenReturn(Optional.of(kb));

        service.delete(kbId);

        verify(vectorStore).delete(any(Filter.Expression.class));
        verify(repository).delete(kb);
    }

    @Test
    void getEntityThrowsWhenMissing() {
        UUID kbId = UUID.randomUUID();
        when(repository.findById(kbId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getEntity(kbId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
