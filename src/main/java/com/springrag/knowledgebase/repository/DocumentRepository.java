package com.springrag.knowledgebase.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
import com.springrag.knowledgebase.domain.DocumentEntity;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {
    List<DocumentEntity> findByKnowledgeBaseId(UUID knowledgeBaseId);
    long countDocumentEntitiesByKnowledgeBaseId(UUID knowledgeBaseId);
}