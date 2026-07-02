package com.springrag.knowledgebase.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import com.springrag.knowledgebase.domain.KnowledgeBase;

public interface KnowledgeBaseRepository
        extends JpaRepository<KnowledgeBase, UUID> {
}