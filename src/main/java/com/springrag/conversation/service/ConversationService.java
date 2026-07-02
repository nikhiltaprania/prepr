package com.springrag.conversation.service;

import java.util.UUID;
import com.springrag.conversation.domain.Conversation;

public interface ConversationService {

    Conversation createConversation(UUID userId, String title);

    Conversation getConversation(UUID conversationId);

}