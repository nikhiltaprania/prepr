package com.springrag.retrieval.dto;

import java.util.List;

/**
 * A knowledge-grounded answer: a natural-language {@code answer} synthesized only from retrieved
 * context, plus the {@code citations} (file + chunk + score) that fed it. Raw chunks remain available
 * via {@code /api/search} for debugging.
 */
public class AskResponse {

    private String answer;
    private List<SourceCitationResponse> citations;

    public AskResponse() {
    }

    public AskResponse(String answer, List<SourceCitationResponse> citations) {
        this.answer = answer;
        this.citations = citations;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<SourceCitationResponse> getCitations() {
        return citations;
    }

    public void setCitations(List<SourceCitationResponse> citations) {
        this.citations = citations;
    }
}
