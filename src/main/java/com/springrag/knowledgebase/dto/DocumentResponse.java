package com.springrag.knowledgebase.dto;

import java.util.UUID;

public class DocumentResponse {

    private UUID id;

    private UUID knowledgeBaseId;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private String status;

    private Integer chunkCount;

    public DocumentResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public void setKnowledgeBaseId(
            UUID knowledgeBaseId) {

        this.knowledgeBaseId =
                knowledgeBaseId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(
            String fileName) {

        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(
            String contentType) {

        this.contentType =
                contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(
            Long fileSize) {

        this.fileSize = fileSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(
            String status) {

        this.status = status;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(
            Integer chunkCount) {

        this.chunkCount =
                chunkCount;
    }
}