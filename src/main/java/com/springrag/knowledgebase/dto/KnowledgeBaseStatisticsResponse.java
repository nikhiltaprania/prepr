package com.springrag.knowledgebase.dto;


public class KnowledgeBaseStatisticsResponse {

    private long documentCount;

    private long chunkCount;

    public KnowledgeBaseStatisticsResponse() {
    }

    public long getDocumentCount() {
        return documentCount;
    }

    public void setDocumentCount(
            long documentCount) {

        this.documentCount =
                documentCount;
    }

    public long getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(
            long chunkCount) {

        this.chunkCount =
                chunkCount;
    }
}