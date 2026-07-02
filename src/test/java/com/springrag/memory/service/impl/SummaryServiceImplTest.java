package com.springrag.memory.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import com.springrag.config.MemoryProperties;
import com.springrag.conversation.domain.Message;
import com.springrag.conversation.domain.MessageRole;
import com.springrag.conversation.service.ConversationService;
import com.springrag.conversation.service.MessageService;
import com.springrag.memory.repository.ConversationSummaryRepository;

class SummaryServiceImplTest {

    private final ConversationService conversationService = mock(ConversationService.class);
    private final MessageService messageService = mock(MessageService.class);
    private final ConversationSummaryRepository summaryRepository = mock(ConversationSummaryRepository.class);
    private final ChatClient chatClient = mock(ChatClient.class);
    private final MemoryProperties memoryProperties = new MemoryProperties();

    private SummaryServiceImpl newService() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        when(builder.build()).thenReturn(chatClient);
        return new SummaryServiceImpl(conversationService, messageService, summaryRepository, builder, memoryProperties);
    }

    private Message message(MessageRole role) {
        Message message = new Message();
        message.setRole(role);
        message.setContent("x");
        return message;
    }

    @Test
    void doesNotRegenerateBelowThreshold() {
        // default threshold is 4; with 2 unsummarized messages it must skip the LLM call entirely
        SummaryServiceImpl service = newService();
        UUID conversationId = UUID.randomUUID();
        when(summaryRepository.findByConversationId(conversationId)).thenReturn(Optional.empty());
        when(messageService.getMessages(conversationId))
                .thenReturn(List.of(message(MessageRole.USER), message(MessageRole.ASSISTANT)));

        service.updateSummary(conversationId);

        verify(summaryRepository, never()).save(any());
        verifyNoInteractions(chatClient);
    }

    @Test
    void getSummaryReturnsEmptyStringWhenNoSummaryStored() {
        SummaryServiceImpl service = newService();
        UUID conversationId = UUID.randomUUID();
        when(summaryRepository.findByConversationId(conversationId)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThat(service.getSummary(conversationId)).isEmpty();
    }
}
