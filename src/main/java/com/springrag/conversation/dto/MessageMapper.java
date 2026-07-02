package com.springrag.conversation.dto;

import com.springrag.conversation.domain.Message;

public final class MessageMapper {

    private MessageMapper() {
    }

    public static MessageResponse toResponse(Message message) {

        MessageResponse response = new MessageResponse();

        response.setId(message.getId());

        response.setRole(message.getRole().name());

        response.setContent(message.getContent());

        response.setCreatedAt(message.getCreatedAt());

        return response;
    }
}