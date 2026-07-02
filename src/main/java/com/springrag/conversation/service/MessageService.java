package com.springrag.conversation.service;

import java.util.List;
import java.util.UUID;
import com.springrag.conversation.domain.Conversation;
import com.springrag.conversation.domain.Message;

public interface MessageService {

    Message saveUserMessage(Conversation conversation, String content);

    Message saveAssistantMessage(Conversation conversation, String content);

    List<Message> getMessages(UUID conversationId);

    /**
     * The most recent {@code limit} messages in chronological (oldest-first) order. Bounded query —
     * use this on per-turn hot paths instead of {@link #getMessages(UUID)}, which loads the entire
     * conversation.
     */
    List<Message> getRecentMessages(UUID conversationId, int limit);
}