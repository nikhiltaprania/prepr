package com.springrag.conversation.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.springrag.conversation.domain.Conversation;
import com.springrag.conversation.domain.Message;
import com.springrag.conversation.domain.MessageRole;
import com.springrag.conversation.dto.CachedMessage;
import com.springrag.conversation.repository.MessageRepository;
import com.springrag.conversation.service.RecentMessageStore;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;
    @Mock
    private RecentMessageStore memoryService;

    @InjectMocks
    private MessageServiceImpl service;

    @Test
    void saveUserMessagePersistsToRepositoryAndWritesThroughToCache() {
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveUserMessage(conversation, "hello");

        verify(messageRepository).save(any(Message.class));

        ArgumentCaptor<CachedMessage> cacheCaptor = ArgumentCaptor.forClass(CachedMessage.class);
        verify(memoryService).addMessage(eq(conversationId), cacheCaptor.capture());
        assertThat(cacheCaptor.getValue().getRole()).isEqualTo(MessageRole.USER.name());
        assertThat(cacheCaptor.getValue().getContent()).isEqualTo("hello");
    }

    @Test
    void saveAssistantMessageWritesAssistantRoleToCache() {
        UUID conversationId = UUID.randomUUID();
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveAssistantMessage(conversation, "hi there");

        ArgumentCaptor<CachedMessage> cacheCaptor = ArgumentCaptor.forClass(CachedMessage.class);
        verify(memoryService).addMessage(eq(conversationId), cacheCaptor.capture());
        assertThat(cacheCaptor.getValue().getRole()).isEqualTo(MessageRole.ASSISTANT.name());
    }
}
