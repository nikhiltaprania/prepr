package com.springrag.knowledgebase.web;

import com.springrag.knowledgebase.dto.CreateKnowledgeBaseRequest;
import com.springrag.knowledgebase.dto.KnowledgeBaseResponse;
import com.springrag.knowledgebase.dto.KnowledgeBaseStatisticsResponse;
import com.springrag.knowledgebase.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/knowledge-bases")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PostMapping
    public KnowledgeBaseResponse create(@Valid @RequestBody CreateKnowledgeBaseRequest request) {
        return knowledgeBaseService.create(request);
    }

    @GetMapping("/{knowledgeBaseId}")
    public KnowledgeBaseResponse get(@PathVariable UUID knowledgeBaseId) {
        return knowledgeBaseService.get(knowledgeBaseId);
    }

    @GetMapping
    public List<KnowledgeBaseResponse> getAll() {
        return knowledgeBaseService.getAll();
    }

    @DeleteMapping("/{knowledgeBaseId}")
    public void delete(@PathVariable UUID knowledgeBaseId) {
        knowledgeBaseService.delete(knowledgeBaseId);
    }

    @GetMapping("/{knowledgeBaseId}/statistics")
    public KnowledgeBaseStatisticsResponse statistics(@PathVariable UUID knowledgeBaseId) {
        return knowledgeBaseService.getStatistics(knowledgeBaseId);
    }
}
