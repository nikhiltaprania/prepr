package com.springrag.knowledgebase.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
import com.springrag.knowledgebase.domain.DocumentChunk;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    List<DocumentChunk> findByDocumentIdOrderByChunkNumberAsc(UUID documentId);
    long countByDocumentId(UUID documentId);

    @Query("select count(c) from DocumentChunk c where c.document.knowledgeBase.id = :knowledgeBaseId")
    long countByKnowledgeBaseId(@Param("knowledgeBaseId") UUID knowledgeBaseId);
}