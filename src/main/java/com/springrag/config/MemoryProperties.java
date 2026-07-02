package com.springrag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

/**
 * Externalized tunables for the memory subsystem. Bound from {@code rag.memory.*}.
 * Replaces previously hard-coded constants scattered across the Redis cache,
 * summary, and semantic-memory services.
 */
@ConfigurationProperties(prefix = "rag.memory")
public class MemoryProperties {

    /**
     * Maximum number of recent messages retained per conversation in the Redis cache.
     */
    private int maxMessages = 20;

    /**
     * Time-to-live for a conversation's Redis cache entry; refreshed on each write.
     */
    private Duration ttl = Duration.ofDays(7);

    /**
     * Number of new (unsummarized) messages that must accumulate before the
     * conversation summary is regenerated.
     */
    private int summaryThreshold = 4;

    /**
     * Number of semantic memories to retrieve per query.
     */
    private int semanticTopK = 5;

    /**
     * Cosine similarity at or above which a candidate memory is treated as a
     * duplicate of an existing one and skipped.
     */
    private double duplicateThreshold = 0.95;

    public int getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public int getSummaryThreshold() {
        return summaryThreshold;
    }

    public void setSummaryThreshold(int summaryThreshold) {
        this.summaryThreshold = summaryThreshold;
    }

    public int getSemanticTopK() {
        return semanticTopK;
    }

    public void setSemanticTopK(int semanticTopK) {
        this.semanticTopK = semanticTopK;
    }

    public double getDuplicateThreshold() {
        return duplicateThreshold;
    }

    public void setDuplicateThreshold(double duplicateThreshold) {
        this.duplicateThreshold = duplicateThreshold;
    }
}
