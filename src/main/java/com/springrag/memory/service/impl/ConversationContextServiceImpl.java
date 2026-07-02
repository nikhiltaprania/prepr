package com.springrag.memory.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import com.springrag.conversation.domain.Message;
import com.springrag.conversation.domain.MessageRole;
import com.springrag.conversation.dto.CachedMessage;
import com.springrag.conversation.repository.MessageRepository;
import com.springrag.conversation.service.RecentMessageStore;
import com.springrag.memory.dto.ContextSource;
import com.springrag.memory.dto.ConversationContext;
import com.springrag.memory.service.ConversationContextService;
import com.springrag.memory.service.SummaryService;

@Service
@Transactional(readOnly = true)
public class ConversationContextServiceImpl
        implements ConversationContextService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ConversationContextServiceImpl.class);

    private final RecentMessageStore memoryService;
    private final SummaryService summaryService;
    private final MessageRepository messageRepository;

    public ConversationContextServiceImpl(
            RecentMessageStore memoryService,
            SummaryService summaryService,
            MessageRepository messageRepository) {

        this.memoryService = memoryService;
        this.summaryService = summaryService;
        this.messageRepository = messageRepository;
    }

    @Override
    public ConversationContext getContext(
            UUID conversationId) {

        ConversationContext context =
                new ConversationContext();

        context.setSummary(
                summaryService.getSummary(
                        conversationId));

        List<CachedMessage> memoryMessages =
                memoryService.getRecentMessages(
                        conversationId);

        if (!memoryMessages.isEmpty()) {

            log.info(
                    "REDIS HIT. conversationId={}",
                    conversationId);

            context.setContextSource(
                    ContextSource.REDIS);

            context.setRecentMessages(
                    memoryMessages.stream()
                            .map(this::toMessage)
                            .toList());

            return context;
        }

        log.info(
                "REDIS MISS. Falling back to PostgreSQL. conversationId={}",
                conversationId);

        List<Message> messages =
                messageRepository
                        .findByConversationIdOrderByCreatedAtAsc(
                                conversationId);

        /*
         * Rebuild Redis cache
         */
        messages.forEach(message ->
                memoryService.addMessage(
                        conversationId,
                        new CachedMessage(
                                message.getRole().name(),
                                message.getContent(),
                                message.getCreatedAt()
                        )
                ));

        context.setContextSource(
                ContextSource.POSTGRESQL);

        context.setRecentMessages(
                messages);

        return context;
    }

    private Message toMessage(
            CachedMessage memory) {

        Message message = new Message();

        message.setRole(
                MessageRole.valueOf(
                        memory.getRole()));

        message.setContent(
                memory.getContent());

        return message;
    }
}