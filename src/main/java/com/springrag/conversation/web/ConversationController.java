package com.springrag.conversation.web;

import com.springrag.conversation.domain.Conversation;
import com.springrag.conversation.dto.ConversationResponse;
import com.springrag.conversation.dto.CreateConversationRequest;
import com.springrag.conversation.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    @PreAuthorize("@access.isCurrentUser(#request.userId) or hasRole('ADMIN')")
    public ConversationResponse createConversation(@Valid @RequestBody CreateConversationRequest request) {
        Conversation conversation = conversationService.createConversation(request.getUserId(), request.getTitle());
        ConversationResponse response = new ConversationResponse();
        response.setConversationId(conversation.getId());
        response.setTitle(conversation.getTitle());
        return response;
    }
}
