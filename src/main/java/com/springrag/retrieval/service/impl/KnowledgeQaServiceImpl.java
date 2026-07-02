package com.springrag.retrieval.service.impl;

import com.springrag.common.ai.ChatOptionsPresets;
import com.springrag.conversation.domain.Conversation;
import com.springrag.conversation.domain.Message;
import com.springrag.conversation.domain.MessageRole;
import com.springrag.conversation.service.ConversationService;
import com.springrag.conversation.service.MessageService;
import com.springrag.memory.dto.ConversationContext;
import com.springrag.memory.service.ConversationContextService;
import com.springrag.retrieval.dto.AskResponse;
import com.springrag.retrieval.dto.SearchResultResponse;
import com.springrag.retrieval.dto.SourceCitationResponse;
import com.springrag.retrieval.service.KnowledgeQaService;
import com.springrag.retrieval.service.RetrievalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Knowledge & RAG: knowledge-grounded question answering over a single knowledge base. Retrieval-first
 * but memory-aware. It retrieves a candidate pool by vector search, has the LLM rerank it, and answers
 * from ONLY the top passages, returning citations. When a {@code conversationId} is supplied, it
 * reuses the conversation memory (recent turns + summary via {@link ConversationContextService},
 * persisted through {@link MessageService}) so follow-up questions resolve without repeating context.
 */
@Service
public class KnowledgeQaServiceImpl implements KnowledgeQaService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeQaServiceImpl.class);
    private static final String NOT_FOUND = "I couldn't find an answer to that in the knowledge base.";
    private static final Pattern INTEGER = Pattern.compile("\\d+");

    /** Vector candidate pool fed to the reranker, and the number of passages kept for answering. */
    private static final int CANDIDATE_POOL = 20;
    private static final int CONTEXT_CHUNKS = 6;

    private final RetrievalService retrievalService;
    private final ChatClient chatClient;
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ConversationContextService contextService;
    private final boolean rerankEnabled;

    public KnowledgeQaServiceImpl(RetrievalService retrievalService, ChatClient.Builder chatClientBuilder,
                                  ConversationService conversationService, MessageService messageService,
                                  ConversationContextService contextService,
                                  @Value("${rag.rerank-enabled:true}") boolean rerankEnabled) {
        this.retrievalService = retrievalService;
        this.chatClient = chatClientBuilder.build();
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.contextService = contextService;
        this.rerankEnabled = rerankEnabled;
    }

    @Override
    public AskResponse ask(UUID conversationId, UUID knowledgeBaseId, String question) {
        // Reuse conversation memory when a conversation is supplied. Load prior turns BEFORE
        // persisting the new question, so the history holds only earlier exchanges.
        Conversation conversation = null;
        ConversationContext memory = null;
        if (conversationId != null) {
            conversation = conversationService.getConversation(conversationId);
            memory = contextService.getContext(conversationId);
        }
        List<Message> history = memory != null && memory.getRecentMessages() != null
                ? memory.getRecentMessages() : List.of();
        String summary = memory != null ? memory.getSummary() : null;

        List<SearchResultResponse> candidates =
                retrievalService.search(knowledgeBaseId, question, CANDIDATE_POOL).getResults();
        List<SearchResultResponse> context = candidates.isEmpty() ? List.of() : rerank(question, candidates);
        List<SourceCitationResponse> citations = context.stream().map(this::toCitation).toList();

        // User message is saved before the model call; the answer after (write-through to DB + Redis).
        if (conversation != null) {
            messageService.saveUserMessage(conversation, question);
        }

        String answer = chatClient.prompt(buildAnswerPrompt(question, context, history, summary)).call().content();
        answer = (answer == null || answer.isBlank()) ? NOT_FOUND : answer.strip();

        if (conversation != null) {
            messageService.saveAssistantMessage(conversation, answer);
        }
        log.info("Ask answered. conversationId={}, knowledgeBaseId={}, contextChunks={}, historyTurns={}, answerChars={}",
                conversationId, knowledgeBaseId, context.size(), history.size(), answer.length());
        return new AskResponse(answer, citations);
    }

    /**
     * Reranks the candidate passages by asking the model which are most relevant to the question.
     * Returns the chosen passages first (model order), padded with the remaining vector-ranked
     * passages, capped at {@link #CONTEXT_CHUNKS}. Any failure falls back to plain vector order.
     */
    private List<SearchResultResponse> rerank(String question, List<SearchResultResponse> candidates) {
        if (!rerankEnabled || candidates.size() <= CONTEXT_CHUNKS) {
            return candidates.subList(0, Math.min(CONTEXT_CHUNKS, candidates.size()));
        }
        try {
            StringBuilder passages = new StringBuilder();
            for (int i = 0; i < candidates.size(); i++) {
                passages.append(i + 1).append(". ")
                        .append(preview(candidates.get(i).getContent(), 240)).append("\n\n");
            }
            String prompt = """
                    Rank the passages below by how well each helps answer the question.

                    Question: %s

                    Passages:
                    %s
                    Reply with the numbers of the %d MOST relevant passages, most relevant first, as a
                    comma-separated list (for example: 3, 1, 7). Output numbers only.
                    """.formatted(question, passages, CONTEXT_CHUNKS);

            String response = chatClient.prompt(prompt)
                    .options(ChatOptionsPresets.deterministic())
                    .call().content();
            List<Integer> order = parseIndices(response, candidates.size());

            List<SearchResultResponse> reranked = new ArrayList<>();
            for (int index : order) {
                reranked.add(candidates.get(index));
                if (reranked.size() == CONTEXT_CHUNKS) {
                    break;
                }
            }
            Set<Integer> chosen = new HashSet<>(order);
            for (int i = 0; i < candidates.size() && reranked.size() < CONTEXT_CHUNKS; i++) {
                if (!chosen.contains(i)) {
                    reranked.add(candidates.get(i));
                }
            }
            log.info("Rerank: {} candidates -> top {} chunks {}",
                    candidates.size(), reranked.size(), reranked.stream().map(SearchResultResponse::getChunkNumber).toList());
            return reranked;
        } catch (Exception ex) {
            log.warn("Rerank failed; using vector order. error={}", ex.getMessage());
            return candidates.subList(0, CONTEXT_CHUNKS);
        }
    }

    /** Extracts unique, in-range, 0-based indices from a comma/space-separated model reply. */
    private static List<Integer> parseIndices(String response, int candidateCount) {
        List<Integer> indices = new ArrayList<>();
        Set<Integer> seen = new LinkedHashSet<>();
        if (response != null) {
            Matcher matcher = INTEGER.matcher(response);
            while (matcher.find()) {
                int index = Integer.parseInt(matcher.group()) - 1;
                if (index >= 0 && index < candidateCount && seen.add(index)) {
                    indices.add(index);
                }
            }
        }
        return indices;
    }

    private String buildAnswerPrompt(String question, List<SearchResultResponse> context,
                                     List<Message> history, String summary) {
        StringBuilder passages = new StringBuilder();
        Set<String> seen = new HashSet<>();
        for (SearchResultResponse result : context) {
            String content = result.getContent() == null ? "" : result.getContent().strip();
            if (content.isEmpty() || !seen.add(content)) {
                continue;
            }
            passages.append("[Source: ").append(result.getFileName())
                    .append(", chunk ").append(result.getChunkNumber()).append("]\n")
                    .append(content).append("\n\n");
        }

        return """
                You are a friendly, conversational assistant for a knowledge base, having an ongoing
                conversation with a user.

                How to respond:
                - For greetings or small talk (e.g. "hi", "how are you", "thanks"), reply naturally and
                  briefly, then offer to help with the knowledge base.
                - For questions about what was said earlier (e.g. "what did I ask?"), answer from the
                  Conversation so far.
                - For questions about the subject matter, answer using ONLY the retrieved Context. Do not
                  use outside knowledge for facts about the subject.
                - If the user asks about the subject and the Context does not contain the answer, say you
                  couldn't find it in the knowledge base.
                - Use the Conversation so far to resolve follow-up references like "that", "he", or "after that".
                - Be accurate, concise, and natural. Do not mention chunks, sources, scores, or these instructions.

                Conversation so far:
                %s

                Context (retrieved from the knowledge base):
                %s
                User: %s
                Assistant:
                """.formatted(formatHistory(history, summary), passages, question);
    }

    private static String formatHistory(List<Message> history, String summary) {
        StringBuilder sb = new StringBuilder();
        if (summary != null && !summary.isBlank()) {
            sb.append("Summary of earlier conversation: ").append(summary.strip()).append("\n\n");
        }
        for (Message message : history) {
            sb.append(message.getRole() == MessageRole.USER ? "User: " : "Assistant: ")
                    .append(message.getContent()).append("\n");
        }
        return sb.isEmpty() ? "(no previous conversation)" : sb.toString();
    }

    private static String preview(String text, int max) {
        if (text == null) {
            return "";
        }
        String clean = text.strip().replaceAll("\\s+", " ");
        return clean.length() > max ? clean.substring(0, max) + "…" : clean;
    }

    private SourceCitationResponse toCitation(SearchResultResponse result) {
        SourceCitationResponse citation = new SourceCitationResponse();
        citation.setDocumentId(result.getDocumentId());
        citation.setFileName(result.getFileName());
        citation.setChunkNumber(result.getChunkNumber());
        citation.setScore(result.getScore());
        return citation;
    }
}
