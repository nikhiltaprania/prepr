package com.springrag.knowledgebase.service;

import com.springrag.knowledgebase.dto.DocumentChunkResponse;
import com.springrag.knowledgebase.dto.DocumentResponse;
import com.springrag.knowledgebase.dto.UploadDocumentResponse;

import java.util.List;
import java.util.UUID;

public interface DocumentIngestionService {

    UploadDocumentResponse ingest(UUID knowledgeBaseId, String fileName, String contentType, byte[] content);

    List<DocumentChunkResponse> getChunks(UUID documentId);

    List<DocumentResponse> getDocuments(UUID knowledgeBaseId);

    DocumentResponse getDocumentResponse(UUID documentId);

    void deleteDocument(UUID documentId);
}
