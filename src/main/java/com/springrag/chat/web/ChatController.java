package com.springrag.chat.web;

import com.springrag.chat.dto.ChatRequest;
import com.springrag.chat.dto.ChatResponse;
import com.springrag.chat.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    @PreAuthorize("@access.ownsConversation(#chatRequest.conversationId) or hasRole('ADMIN')")
    public ChatResponse chat(@Valid @RequestBody ChatRequest chatRequest) {
        return chatService.chat(chatRequest.getConversationId(), chatRequest.getMessage());
    }
}
