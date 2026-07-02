package com.springrag.memory.web;

import com.springrag.conversation.dto.MessageMapper;
import com.springrag.memory.dto.ContextResponse;
import com.springrag.memory.dto.ConversationContext;
import com.springrag.memory.service.ConversationContextService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
public class ContextController {

    private final ConversationContextService contextService;

    public ContextController(ConversationContextService contextService) {
        this.contextService = contextService;
    }

    @GetMapping("/{conversationId}/context")
    @PreAuthorize("@access.ownsConversation(#conversationId) or hasRole('ADMIN')")
    public ContextResponse getContext(@PathVariable UUID conversationId) {
        ConversationContext context = contextService.getContext(conversationId);
        ContextResponse response = new ContextResponse();
        response.setSource(context.getContextSource().name());
        response.setSummary(context.getSummary());
        response.setRecentMessages(context.getRecentMessages().stream().map(MessageMapper::toResponse).toList());
        return response;
    }
}
