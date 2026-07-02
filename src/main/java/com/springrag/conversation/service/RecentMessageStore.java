package com.springrag.conversation.service;

import java.util.List;
import java.util.UUID;
import com.springrag.conversation.dto.CachedMessage;

public interface RecentMessageStore {

    void addMessage(UUID conversationId, CachedMessage message);

    List<CachedMessage> getRecentMessages(UUID conversationId);

    void clearMemory(UUID conversationId);
}