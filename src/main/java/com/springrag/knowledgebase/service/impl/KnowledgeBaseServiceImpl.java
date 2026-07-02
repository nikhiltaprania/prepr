package com.springrag.knowledgebase.service.impl;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import com.springrag.common.exception.ResourceNotFoundException;
import com.springrag.knowledgebase.domain.KnowledgeBase;
import com.springrag.knowledgebase.dto.CreateKnowledgeBaseRequest;
import com.springrag.knowledgebase.dto.KnowledgeBaseResponse;
import com.springrag.knowledgebase.dto.KnowledgeBaseStatisticsResponse;
import com.springrag.knowledgebase.repository.DocumentChunkRepository;
import com.springrag.knowledgebase.repository.DocumentRepository;
import com.springrag.knowledgebase.repository.KnowledgeBaseRepository;
import com.springrag.knowledgebase.service.KnowledgeBaseService;

@Service
@Transactional
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseRepository repository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final VectorStore vectorStore;

    public KnowledgeBaseServiceImpl(KnowledgeBaseRepository repository, DocumentRepository documentRepository, DocumentChunkRepository documentChunkRepository, VectorStore vectorStore) {
        this.repository = repository;
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.vectorStore = vectorStore;
    }

    @Override
    public KnowledgeBaseResponse create(CreateKnowledgeBaseRequest request) {

        KnowledgeBase knowledgeBase = new KnowledgeBase();

        knowledgeBase.setName(request.getName());

        knowledgeBase.setDescription(request.getDescription());

        knowledgeBase.setActive(true);

        knowledgeBase = repository.save(knowledgeBase);

        return toResponse(knowledgeBase);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBaseResponse get(UUID knowledgeBaseId) {

        return toResponse(getEntity(knowledgeBaseId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseResponse> getAll() {

        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public void delete(UUID knowledgeBaseId) {

        KnowledgeBase knowledgeBase = getEntity(knowledgeBaseId);

        // Remove this knowledge base's embeddings from the vector store; the relational
        // documents and chunks are removed by JPA cascade/orphanRemoval.
        FilterExpressionBuilder filter = new FilterExpressionBuilder();
        vectorStore.delete(filter.eq("knowledgeBaseId", knowledgeBaseId.toString()).build());

        repository.delete(knowledgeBase);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBase getEntity(UUID knowledgeBaseId) {

        return repository.findById(knowledgeBaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge Base not found: " + knowledgeBaseId));
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBaseStatisticsResponse getStatistics(UUID knowledgeBaseId) {

        KnowledgeBaseStatisticsResponse response = new KnowledgeBaseStatisticsResponse();

        long documents = documentRepository.countDocumentEntitiesByKnowledgeBaseId(knowledgeBaseId);

        long chunks = documentChunkRepository.countByKnowledgeBaseId(knowledgeBaseId);

        response.setDocumentCount(documents);

        response.setChunkCount(chunks);

        return response;
    }

    private KnowledgeBaseResponse toResponse(KnowledgeBase entity) {

        KnowledgeBaseResponse response = new KnowledgeBaseResponse();

        response.setId(entity.getId());

        response.setName(entity.getName());

        response.setDescription(entity.getDescription());

        response.setActive(entity.isActive());

        response.setCreatedAt(entity.getCreatedAt());

        return response;
    }
}