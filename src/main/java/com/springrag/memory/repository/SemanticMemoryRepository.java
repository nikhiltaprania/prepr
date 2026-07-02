package com.springrag.memory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;
import com.springrag.memory.domain.MemoryCategory;
import com.springrag.memory.domain.SemanticMemory;

public interface SemanticMemoryRepository
        extends JpaRepository<
        SemanticMemory,
        UUID> {

    List<SemanticMemory> findByConversationId(UUID conversationId);
    long countByUserId(UUID userId);

    long countByUserIdAndCategory(UUID userId, MemoryCategory category);
}