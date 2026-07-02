package com.springrag.retrieval.service.impl;

import com.springrag.common.ai.EmbeddingPrefixes;
import com.springrag.retrieval.dto.SearchResponse;
import com.springrag.retrieval.dto.SearchResultResponse;
import com.springrag.retrieval.service.RetrievalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Vector similarity search over a single knowledge base. The query and the stored chunks are
 * embedded by the same autoconfigured embedding model (inside {@link VectorStore#similaritySearch});
 * results are restricted to the requested knowledge base via a metadata filter on
 * {@code knowledgeBaseId}.
 *
 * <p>The query is embedded exactly once — by the vector store. (An earlier version also embedded the
 * query separately for diagnostic logging, doubling the per-search embedding cost on the hottest RAG
 * path; that diagnostic was removed.) Detailed ranked-chunk tracing is emitted at DEBUG only.
 */
@Service
public class RetrievalServiceImpl implements RetrievalService {

    private static final Logger log = LoggerFactory.getLogger(RetrievalServiceImpl.class);
    private static final int DEFAULT_TOP_K = 10;
    private static final String KB_METADATA_KEY = "knowledgeBaseId";

    private final VectorStore vectorStore;

    public RetrievalServiceImpl(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public SearchResponse search(UUID knowledgeBaseId, String query) {
        return search(knowledgeBaseId, query, DEFAULT_TOP_K);
    }

    @Override
    public SearchResponse search(UUID knowledgeBaseId, String query, int topK) {
        Filter.Expression filter = new FilterExpressionBuilder()
                .eq(KB_METADATA_KEY, knowledgeBaseId.toString())
                .build();

        // Nomic Embed requires the query to carry the "search_query:" prefix so it matches how the
        // chunks were embedded ("search_document:"). The prefix affects only the embedding, never display.
        // The vector store embeds this string once internally — we do NOT pre-embed it ourselves.
        String embeddingQuery = EmbeddingPrefixes.QUERY + query;

        log.debug("Semantic search start. knowledgeBaseId={}, topK={}", knowledgeBaseId, topK);

        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(embeddingQuery)
                .topK(topK)
                .filterExpression(filter)
                .build());

        if (documents.isEmpty()) {
            log.warn("Semantic search returned 0 chunks. knowledgeBaseId={}. " +
                    "Verify documents were ingested into THIS knowledge base and that each chunk's " +
                    "'{}' metadata equals the requested id.", knowledgeBaseId, KB_METADATA_KEY);
        } else if (log.isDebugEnabled()) {
            log.debug("Semantic search returned {} chunk(s) for knowledgeBaseId={}:", documents.size(), knowledgeBaseId);
            for (int i = 0; i < documents.size(); i++) {
                Document d = documents.get(i);
                log.debug("  #{} score={} kb={} doc={} file={} chunk={} preview='{}'",
                        i,
                        d.getScore(),
                        d.getMetadata().get(KB_METADATA_KEY),
                        d.getMetadata().get("documentId"),
                        d.getMetadata().get("fileName"),
                        d.getMetadata().get("chunkNumber"),
                        preview(stripDocumentPrefix(d.getText())));
            }
        }

        List<SearchResultResponse> results = documents.stream().map(this::toResult).toList();

        SearchResponse response = new SearchResponse();
        response.setQuery(query);
        response.setResultCount(results.size());
        response.setResults(results);
        return response;
    }

    private SearchResultResponse toResult(Document document) {
        SearchResultResponse response = new SearchResultResponse();
        response.setContent(stripDocumentPrefix(document.getText()));
        response.setDocumentId(asString(document.getMetadata().get("documentId")));
        response.setFileName(asString(document.getMetadata().get("fileName")));
        response.setChunkNumber(asInt(document.getMetadata().get("chunkNumber")));
        // The similarity score lives on the Document (higher = more similar), not in metadata.
        response.setScore(document.getScore());
        return response;
    }

    private static String stripDocumentPrefix(String text) {
        if (text != null && text.startsWith(EmbeddingPrefixes.DOCUMENT)) {
            return text.substring(EmbeddingPrefixes.DOCUMENT.length());
        }
        return text;
    }

    private static String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private static Integer asInt(Object value) {
        if (value == null) return null;
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String preview(String text) {
        if (text == null) return "";
        String clean = text.strip().replaceAll("\\s+", " ");
        return clean.length() > 120 ? clean.substring(0, 120) + "…" : clean;
    }
}
