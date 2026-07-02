package com.springrag.chat.service;

import org.springframework.ai.chat.prompt.Prompt;
import com.springrag.memory.dto.ConversationContext;

public interface PromptService {

    Prompt buildPrompt(
            ConversationContext context);

}