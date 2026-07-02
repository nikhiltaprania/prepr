package com.springrag.conversation.web;

import com.springrag.conversation.dto.MessageMapper;
import com.springrag.conversation.dto.MessageResponse;
import com.springrag.conversation.service.MessageService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/{conversationId}/messages")
    @PreAuthorize("@access.ownsConversation(#conversationId) or hasRole('ADMIN')")
    public List<MessageResponse> getMessages(@PathVariable UUID conversationId) {
        return messageService.getMessages(conversationId)
                .stream()
                .map(MessageMapper::toResponse)
                .toList();
    }
}
