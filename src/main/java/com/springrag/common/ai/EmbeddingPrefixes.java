package com.springrag.common.ai;

/**
 * Task prefixes required by the Nomic Embed text models (e.g. nomic-embed-text-v2). Documents and
 * queries MUST be embedded with their matching prefix or retrieval quality drops sharply. The
 * document prefix is applied only to the text sent to the embedder/vector store and is stripped
 * before the chunk is shown to users or injected into prompts.
 */
public final class EmbeddingPrefixes {

    private EmbeddingPrefixes() {
    }

    public static final String DOCUMENT = "search_document: ";
    public static final String QUERY = "search_query: ";
}
