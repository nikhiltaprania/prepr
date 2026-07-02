package com.springrag.conversation.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import com.springrag.conversation.domain.Conversation;
import com.springrag.conversation.domain.Message;
import com.springrag.conversation.domain.MessageRole;
import com.springrag.conversation.dto.CachedMessage;
import com.springrag.conversation.repository.MessageRepository;
import com.springrag.conversation.service.MessageService;
import com.springrag.conversation.service.RecentMessageStore;

/**
 * Owns message persistence. PostgreSQL is the source of truth; the Redis short-term
 * cache is kept consistent by writing through on every save, so callers persist a
 * message in one call instead of updating both stores by hand.
 */
@Service
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final RecentMessageStore memoryService;

    public MessageServiceImpl(MessageRepository messageRepository, RecentMessageStore memoryService) {

        this.messageRepository = messageRepository;
        this.memoryService = memoryService;
    }

    @Override
    public Message saveUserMessage(Conversation conversation, String content) {

        return saveMessage(conversation, MessageRole.USER, content);
    }

    @Override
    public Message saveAssistantMessage(Conversation conversation, String content) {

        return saveMessage(conversation, MessageRole.ASSISTANT, content);
    }

    private Message saveMessage(Conversation conversation, MessageRole role, String content) {

        Message message = new Message();
        message.setConversation(conversation);
        message.setRole(role);
        message.setContent(content);

        Message saved = messageRepository.save(message);

        memoryService.addMessage(
                conversation.getId(),
                new CachedMessage(role.name(), content, LocalDateTime.now()));

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getMessages(UUID conversationId) {

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getRecentMessages(UUID conversationId, int limit) {

        List<Message> recent = new ArrayList<>(messageRepository
                .findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(0, limit)));
        Collections.reverse(recent);
        return recent;
    }
}