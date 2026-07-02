package com.springrag.memory.service;

import java.util.UUID;
import com.springrag.memory.dto.ConversationContext;

public interface ConversationContextService {

    ConversationContext getContext(
            UUID conversationId);

}