package com.springrag.knowledgebase.web;

import com.springrag.knowledgebase.dto.DocumentChunkResponse;
import com.springrag.knowledgebase.dto.DocumentResponse;
import com.springrag.knowledgebase.dto.UploadDocumentResponse;
import com.springrag.knowledgebase.service.DocumentIngestionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentIngestionService documentIngestionService;

    public DocumentController(DocumentIngestionService documentIngestionService) {
        this.documentIngestionService = documentIngestionService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadDocumentResponse upload(@RequestParam UUID knowledgeBaseId,
                                         @RequestPart("file") MultipartFile file) {
        String contentType = file.getContentType() != null
                ? file.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        return documentIngestionService.ingest(knowledgeBaseId, file.getOriginalFilename(), contentType, readBytes(file));
    }

    @GetMapping("/{documentId}")
    public DocumentResponse getDocument(@PathVariable UUID documentId) {
        return documentIngestionService.getDocumentResponse(documentId);
    }

    @GetMapping("/{documentId}/chunks")
    public List<DocumentChunkResponse> getChunks(@PathVariable UUID documentId) {
        return documentIngestionService.getChunks(documentId);
    }

    @GetMapping("/knowledge-base/{knowledgeBaseId}")
    public List<DocumentResponse> getDocuments(@PathVariable UUID knowledgeBaseId) {
        return documentIngestionService.getDocuments(knowledgeBaseId);
    }

    @DeleteMapping("/{documentId}")
    public void deleteDocument(@PathVariable UUID documentId) {
        documentIngestionService.deleteDocument(documentId);
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read uploaded file", ex);
        }
    }
}
