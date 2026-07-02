package com.springrag.chat.service.impl;

import com.springrag.chat.dto.ChatResponse;
import com.springrag.chat.service.ChatService;
import com.springrag.chat.service.PromptService;
import com.springrag.conversation.domain.Conversation;
import com.springrag.conversation.service.ConversationService;
import com.springrag.conversation.service.MessageService;
import com.springrag.memory.dto.ConversationContext;
import com.springrag.memory.service.ConversationContextService;
import com.springrag.memory.service.SemanticMemoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * A conversational, memory-first AI assistant (like a general-purpose chat). A turn persists the user
 * message, assembles context from the conversation's recent history, summary, and the user's
 * long-term semantic memories, calls the model, then persists the answer, refreshes the summary, and
 * extracts new long-term memories. It does NOT use a knowledge base, perform document retrieval, or
 * return citations — that is the Knowledge & RAG module ({@code /api/ask}).
 *
 * Not {@code @Transactional}: each collaborator owns a short transaction, so no JPA connection is
 * held across the LLM calls. Memory extraction runs after generation (it targets only past memories).
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);

    private final ChatClient chatClient;
    private final PromptService promptService;
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ConversationContextService contextService;
    private final SemanticMemoryService semanticMemoryService;
    private final ChatPostProcessor chatPostProcessor;

    public ChatServiceImpl(ChatClient.Builder chatClientBuilder, PromptService promptService,
                           ConversationService conversationService, MessageService messageService,
                           ConversationContextService contextService,
                           SemanticMemoryService semanticMemoryService,
                           ChatPostProcessor chatPostProcessor) {
        this.chatClient = chatClientBuilder.build();
        this.promptService = promptService;
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.contextService = contextService;
        this.semanticMemoryService = semanticMemoryService;
        this.chatPostProcessor = chatPostProcessor;
    }

    @Override
    public ChatResponse chat(UUID conversationId, String userMessage) {
        long startTime = System.currentTimeMillis();
        log.info("Processing chat request. conversationId={}", conversationId);

        ChatTurn turn = prepare(conversationId, userMessage);

        String response = chatClient.prompt(turn.prompt).call().content();

        complete(turn, response);

        log.info("Chat completed. conversationId={}, executionTime={} ms",
                conversationId, System.currentTimeMillis() - startTime);
        return new ChatResponse(response);
    }

    /** Persist the user message and assemble the memory-first prompt (recent + summary + memories). */
    private ChatTurn prepare(UUID conversationId, String userMessage) {
        Conversation conversation = conversationService.getConversation(conversationId);
        messageService.saveUserMessage(conversation, userMessage);
        UUID userId = conversation.getUser().getId();

        ConversationContext context = contextService.getContext(conversationId);
        context.setMemoryResults(semanticMemoryService.retrieveMemories(userId, userMessage));

        log.debug("Chat context. source={}, recent={}, memories={}",
                context.getContextSource(), context.getRecentMessages().size(),
                context.getMemoryResults().size());

        Prompt prompt = promptService.buildPrompt(context);
        return new ChatTurn(conversation, userId, conversationId, userMessage, prompt);
    }

    /**
     * Persist the answer, then hand summary refresh + long-term memory extraction to a background
     * thread — neither affects the response the caller already has, and both are extra LLM/vector
     * calls that would otherwise inflate turn latency.
     */
    private void complete(ChatTurn turn, String response) {
        messageService.saveAssistantMessage(turn.conversation, response);
        chatPostProcessor.process(turn.userId, turn.conversationId, turn.userMessage);
    }

    private static final class ChatTurn {
        private final Conversation conversation;
        private final UUID userId;
        private final UUID conversationId;
        private final String userMessage;
        private final Prompt prompt;

        private ChatTurn(Conversation conversation, UUID userId, UUID conversationId,
                         String userMessage, Prompt prompt) {
            this.conversation = conversation;
            this.userId = userId;
            this.conversationId = conversationId;
            this.userMessage = userMessage;
            this.prompt = prompt;
        }
    }
}
