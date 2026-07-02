package com.springrag.knowledgebase.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.springrag.common.ai.EmbeddingPrefixes;
import com.springrag.common.exception.ResourceNotFoundException;
import com.springrag.common.io.NamedByteArrayResource;
import com.springrag.knowledgebase.domain.DocumentChunk;
import com.springrag.knowledgebase.domain.DocumentEntity;
import com.springrag.knowledgebase.domain.DocumentStatus;
import com.springrag.knowledgebase.domain.KnowledgeBase;
import com.springrag.knowledgebase.dto.DocumentChunkResponse;
import com.springrag.knowledgebase.dto.DocumentResponse;
import com.springrag.knowledgebase.dto.UploadDocumentResponse;
import com.springrag.knowledgebase.repository.DocumentChunkRepository;
import com.springrag.knowledgebase.repository.DocumentRepository;
import com.springrag.knowledgebase.service.DocumentIngestionService;
import com.springrag.knowledgebase.service.KnowledgeBaseService;

@Service
@Transactional
public class DocumentIngestionServiceImpl implements DocumentIngestionService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    DocumentIngestionServiceImpl.class);

    private final DocumentRepository documentRepository;

    private final DocumentChunkRepository chunkRepository;

    private final VectorStore vectorStore;
    private final KnowledgeBaseService knowledgeBaseService;

    public DocumentIngestionServiceImpl(
            DocumentRepository documentRepository,
            DocumentChunkRepository chunkRepository,
            VectorStore vectorStore,
            KnowledgeBaseService knowledgeBaseService) {

        this.documentRepository =
                documentRepository;

        this.chunkRepository =
                chunkRepository;

        this.vectorStore =
                vectorStore;

        this.knowledgeBaseService =
                knowledgeBaseService;
    }

    @Override
    public UploadDocumentResponse ingest(
            UUID knowledgeBaseId,
            String fileName,
            String contentType,
            byte[] content) {

        try {

            KnowledgeBase knowledgeBase =
                    knowledgeBaseService.getEntity(
                            knowledgeBaseId);

            log.info(
                    "Starting ingestion. file={}",
                    fileName);

            DocumentEntity document =
                    new DocumentEntity();

            document.setKnowledgeBase(
                    knowledgeBase);

            document.setFileName(
                    fileName);

            document.setContentType(
                    contentType);

            document.setFileSize(
                    (long) content.length);

            document.setStatus(
                    DocumentStatus.PROCESSING);

            document =
                    documentRepository.save(
                            document);

            Resource resource =
                    new NamedByteArrayResource(
                            content,
                            fileName);

            log.info(
                    "Reading document using Spring AI TikaDocumentReader");

            TikaDocumentReader reader =
                    new TikaDocumentReader(
                            resource);

            List<Document> documents =
                    reader.get();

            log.info(
                    "Reader extracted {} documents",
                    documents.size());

            // Smaller chunks keep a single fact from being diluted across a large averaged embedding,
            // which improves precise fact retrieval.
            TokenTextSplitter splitter =
                    TokenTextSplitter.builder()
                            .withChunkSize(300)
                            .withMinChunkSizeChars(120)
                            .withMinChunkLengthToEmbed(20)
                            .withMaxNumChunks(10000)
                            .withKeepSeparator(true)
                            .build();

            List<Document> chunks =
                    splitter.apply(
                            documents);

            log.info(
                    "Created {} chunks",
                    chunks.size());

            List<DocumentChunk> chunkEntities =
                    new ArrayList<>();

            List<Document> vectorDocuments =
                    new ArrayList<>();

            int chunkNumber = 1;

            for (Document chunk : chunks) {

                DocumentChunk entity =
                        new DocumentChunk();

                entity.setDocument(
                        document);

                entity.setChunkNumber(
                        chunkNumber);

                entity.setContent(
                        chunk.getText());

                entity.setSource(
                        fileName);

                chunkEntities.add(entity);

                Document vectorDocument =
                        new Document(
                                EmbeddingPrefixes.DOCUMENT + chunk.getText(),
                                Map.of(
                                        "documentId",
                                        document.getId().toString(),

                                        "fileName",
                                        document.getFileName(),

                                        "chunkNumber",
                                        chunkNumber,

                                        "knowledgeBaseId",
                                        knowledgeBase.getId().toString(),

                                        "knowledgeBaseName",
                                        knowledgeBase.getName()
                                )
                        );

                vectorDocuments.add(
                        vectorDocument);

                chunkNumber++;
            }

            chunkRepository.saveAll(chunkEntities);

            log.info("Embedding + storing {} chunks into VectorStore. knowledgeBaseId={}, documentId={}",
                    vectorDocuments.size(), knowledgeBase.getId(), document.getId());
            if (log.isDebugEnabled()) {
                for (Document vd : vectorDocuments) {
                    String text = vd.getText() == null ? "" : vd.getText();
                    String clean = text.strip().replaceAll("\\s+", " ");
                    String preview = clean.length() > 120 ? clean.substring(0, 120) + "…" : clean;
                    log.debug("  ingest chunk kb={} doc={} chunk={} chars={} preview='{}'",
                            vd.getMetadata().get("knowledgeBaseId"), vd.getMetadata().get("documentId"),
                            vd.getMetadata().get("chunkNumber"), text.length(), preview);
                }
            }

            vectorStore.add(vectorDocuments);

            log.info("VectorStore ingestion completed. knowledgeBaseId={}, documentId={}",
                    knowledgeBase.getId(), document.getId());

            document.setStatus(
                    DocumentStatus.PROCESSED);

            documentRepository.save(
                    document);

            UploadDocumentResponse response =
                    new UploadDocumentResponse();

            response.setDocumentId(
                    document.getId());

            response.setFileName(
                    document.getFileName());

            response.setChunkCount(
                    chunks.size());
            response.setKnowledgeBaseId(
                    knowledgeBase.getId());

            log.info(
                    "Document processed successfully. documentId={}, chunks={}",
                    document.getId(),
                    chunks.size());

            return response;

        } catch (Exception ex) {

            log.error(
                    "Document ingestion failed",
                    ex);

            throw new RuntimeException(
                    "Document ingestion failed",
                    ex);
        }
    }

    private DocumentEntity getDocument(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentChunkResponse> getChunks(
            UUID documentId) {

        if (!documentRepository.existsById(
                documentId)) {

            throw new ResourceNotFoundException(
                    "Document not found: "
                            + documentId);
        }

        return chunkRepository
                .findByDocumentIdOrderByChunkNumberAsc(
                        documentId)
                .stream()
                .map(this::toChunkResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments(
            UUID knowledgeBaseId) {

        return documentRepository
                .findByKnowledgeBaseId(
                        knowledgeBaseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getDocumentResponse(
            UUID documentId) {

        return toResponse(
                getDocument(
                        documentId));
    }

    @Override
    public void deleteDocument(
            UUID documentId) {

        DocumentEntity document =
                getDocument(
                        documentId);

        // Remove this document's embeddings from the vector store; the relational
        // chunks are removed by JPA cascade/orphanRemoval.
        FilterExpressionBuilder filter = new FilterExpressionBuilder();
        vectorStore.delete(filter.eq("documentId", documentId.toString()).build());

        documentRepository.delete(
                document);
    }

    private DocumentChunkResponse toChunkResponse(
            DocumentChunk chunk) {

        DocumentChunkResponse response =
                new DocumentChunkResponse();

        response.setId(chunk.getId());
        response.setDocumentId(chunk.getDocument().getId());
        response.setChunkNumber(chunk.getChunkNumber());
        response.setContent(chunk.getContent());
        response.setSource(chunk.getSource());

        return response;
    }

    private DocumentResponse toResponse(
            DocumentEntity document) {

        DocumentResponse response =
                new DocumentResponse();

        response.setId(
                document.getId());

        response.setKnowledgeBaseId(
                document.getKnowledgeBase()
                        .getId());

        response.setFileName(
                document.getFileName());

        response.setContentType(
                document.getContentType());

        response.setFileSize(
                document.getFileSize());

        response.setStatus(
                document.getStatus()
                        .name());

        response.setChunkCount(
                (int) chunkRepository
                        .countByDocumentId(
                                document.getId()));

        return response;
    }
}