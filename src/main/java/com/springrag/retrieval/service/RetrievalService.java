package com.springrag.retrieval.service;

import com.springrag.retrieval.dto.SearchResponse;

import java.util.UUID;

public interface RetrievalService {

    /** Vector search with the default result count. */
    SearchResponse search(UUID knowledgeBaseId, String query);

    /** Vector search returning up to {@code topK} chunks (used to build a rerank candidate pool). */
    SearchResponse search(UUID knowledgeBaseId, String query, int topK);
}
