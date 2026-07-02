package com.springrag.knowledgebase.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;
import java.util.List;
import com.springrag.knowledgebase.dto.KnowledgeBaseResponse;
import com.springrag.knowledgebase.service.KnowledgeBaseService;

@Component
public class KnowledgeBaseTool {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseTool(KnowledgeBaseService knowledgeBaseService) {

        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Tool(description = "Get all knowledge bases")
    public List<KnowledgeBaseResponse> getKnowledgeBases() {

        return knowledgeBaseService.getAll();
    }
}