package com.springrag.retrieval.dto;

import java.util.List;

public class SearchResponse {

    private String query;

    private Integer resultCount;

    private List<SearchResultResponse> results;

    public SearchResponse() {
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }

    public List<SearchResultResponse> getResults() {
        return results;
    }

    public void setResults(
            List<SearchResultResponse> results) {

        this.results = results;
    }
}