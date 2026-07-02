package com.springrag.retrieval.web;

import com.springrag.retrieval.dto.AskRequest;
import com.springrag.retrieval.dto.AskResponse;
import com.springrag.retrieval.service.KnowledgeQaService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Knowledge & RAG grounded Q&A: returns a knowledge-grounded answer with citations. For raw
 * chunks/scores/metadata use {@code POST /api/search}; for memory-only chat use {@code POST /api/chat}.
 */
@RestController
@RequestMapping("/api/ask")
public class KnowledgeQaController {

    private final KnowledgeQaService knowledgeQaService;

    public KnowledgeQaController(KnowledgeQaService knowledgeQaService) {
        this.knowledgeQaService = knowledgeQaService;
    }

    @PostMapping
    @PreAuthorize("@access.ownsConversation(#request.conversationId) or hasRole('ADMIN')")
    public AskResponse ask(@Valid @RequestBody AskRequest request) {
        return knowledgeQaService.ask(request.getConversationId(), request.getKnowledgeBaseId(), request.getQuestion());
    }
}
