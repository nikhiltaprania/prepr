package com.springrag.retrieval.web;

import com.springrag.retrieval.dto.SearchRequest;
import com.springrag.retrieval.dto.SearchResponse;
import com.springrag.retrieval.service.RetrievalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private final RetrievalService retrievalService;

    public SearchController(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @PostMapping
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {
        return retrievalService.search(request.getKnowledgeBaseId(), request.getQuery());
    }
}
