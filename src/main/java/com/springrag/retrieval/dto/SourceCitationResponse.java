package com.springrag.retrieval.dto;

/**
 * A citation for a grounded answer: the source document/chunk and its relevance score.
 */
public class SourceCitationResponse {

    private String documentId;
    private String fileName;
    private Integer chunkNumber;
    private Double score;

    public SourceCitationResponse() {
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getChunkNumber() {
        return chunkNumber;
    }

    public void setChunkNumber(Integer chunkNumber) {
        this.chunkNumber = chunkNumber;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
