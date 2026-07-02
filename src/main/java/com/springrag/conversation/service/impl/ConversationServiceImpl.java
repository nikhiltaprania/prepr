package com.springrag.conversation.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import com.springrag.common.exception.ResourceNotFoundException;
import com.springrag.conversation.domain.Conversation;
import com.springrag.conversation.repository.ConversationRepository;
import com.springrag.conversation.service.ConversationService;
import com.springrag.user.domain.User;
import com.springrag.user.repository.UserRepository;

@Service
@Transactional
public class ConversationServiceImpl implements ConversationService {
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;

    public ConversationServiceImpl(UserRepository userRepository, ConversationRepository conversationRepository) {
        this.userRepository = userRepository;
        this.conversationRepository = conversationRepository;
    }

    @Override
    public Conversation createConversation(UUID userId, String title) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Conversation conversation = new Conversation();
        conversation.setUser(user);
        conversation.setTitle(title);
        conversation.setActive(true);

        return conversationRepository.save(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public Conversation getConversation(UUID conversationId) {

        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
    }
}