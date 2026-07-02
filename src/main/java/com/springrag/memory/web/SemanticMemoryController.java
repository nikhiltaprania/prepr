package com.springrag.memory.web;

import com.springrag.memory.dto.MemoryStatisticsResponse;
import com.springrag.memory.service.SemanticMemoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/memories")
public class SemanticMemoryController {

    private final SemanticMemoryService memoryService;

    public SemanticMemoryController(SemanticMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @GetMapping("/statistics/{userId}")
    @PreAuthorize("@access.isCurrentUser(#userId) or hasRole('ADMIN')")
    public MemoryStatisticsResponse statistics(@PathVariable UUID userId) {
        return memoryService.getStatistics(userId);
    }
}
