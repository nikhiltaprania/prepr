package com.springrag.memory.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
import com.springrag.config.MemoryProperties;
import com.springrag.conversation.domain.Conversation;
import com.springrag.conversation.domain.Message;
import com.springrag.conversation.service.ConversationService;
import com.springrag.conversation.service.MessageService;
import com.springrag.memory.domain.ConversationSummary;
import com.springrag.memory.repository.ConversationSummaryRepository;
import com.springrag.memory.service.SummaryService;

/**
 * Generates conversation summaries. Not {@code @Transactional}: messages are read in their own
 * short transactions, the summary LLM call runs with no transaction held, and the summary is
 * upserted in a final short {@code save}.
 */
@Service
public class SummaryServiceImpl implements SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryServiceImpl.class);

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ConversationSummaryRepository summaryRepository;
    private final ChatClient chatClient;
    private final MemoryProperties memoryProperties;

    public SummaryServiceImpl(ConversationService conversationService, MessageService messageService, ConversationSummaryRepository summaryRepository, ChatClient.Builder chatClientBuilder, MemoryProperties memoryProperties) {
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.summaryRepository = summaryRepository;
        this.chatClient = chatClientBuilder.build();
        this.memoryProperties = memoryProperties;
    }

    @Override
    public String getSummary(UUID conversationId) {

        return summaryRepository.findByConversationId(conversationId).map(ConversationSummary::getSummary).orElse("");
    }

    @Override
    public void updateSummary(UUID conversationId) {

        ConversationSummary summaryEntity = summaryRepository.findByConversationId(conversationId).orElse(null);

        int summarizedCount = summaryEntity == null ? 0 : summaryEntity.getSummarizedMessageCount();

        List<Message> messages = messageService.getMessages(conversationId);

        int totalMessages = messages.size();

        int unsummarizedMessages = totalMessages - summarizedCount;

        if (unsummarizedMessages < memoryProperties.getSummaryThreshold()) {
            return;
        }

        regenerateSummary(conversationId);
    }

    @Override
    public void regenerateSummary(UUID conversationId) {

        Conversation conversation = conversationService.getConversation(conversationId);

        List<Message> messages = messageService.getMessages(conversationId);

        StringBuilder conversationText = new StringBuilder();

        for (Message message : messages) {

            conversationText.append(message.getRole().name());

            conversationText.append(": ");

            conversationText.append(message.getContent());

            conversationText.append("\n");
        }

        String prompt = """
                Create a concise conversation summary.
                
                Include:
                
                - Important facts
                - User preferences
                - Important decisions
                - Key discussion topics
                
                Conversation:
                
                %s
                """.formatted(conversationText);

        String summary = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        log.debug("Generated summary for conversation {}: {}", conversationId, summary);

        ConversationSummary entity = summaryRepository.findByConversationId(conversationId).orElseGet(ConversationSummary::new);

        entity.setConversation(conversation);
        entity.setSummary(summary);
        entity.setSummarizedMessageCount(messages.size());

        summaryRepository.save(entity);
    }
}