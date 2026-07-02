package com.springrag.knowledgebase.service;

import java.util.List;
import java.util.UUID;
import com.springrag.knowledgebase.domain.KnowledgeBase;
import com.springrag.knowledgebase.dto.CreateKnowledgeBaseRequest;
import com.springrag.knowledgebase.dto.KnowledgeBaseResponse;
import com.springrag.knowledgebase.dto.KnowledgeBaseStatisticsResponse;

public interface KnowledgeBaseService {

    KnowledgeBaseResponse create(
            CreateKnowledgeBaseRequest request);

    KnowledgeBaseResponse get(
            UUID knowledgeBaseId);

    List<KnowledgeBaseResponse> getAll();

    void delete(
            UUID knowledgeBaseId);

    KnowledgeBase getEntity(
            UUID knowledgeBaseId);

    KnowledgeBaseStatisticsResponse
    getStatistics(
            UUID knowledgeBaseId);
}