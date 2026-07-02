package com.springrag.conversation.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
import com.springrag.conversation.domain.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    /**
     * The most recent messages for a conversation, newest-first. Backed by the
     * {@code (conversation_id)} index plus a LIMIT, so per-turn history loading stays O(window)
     * instead of scanning the whole conversation. Callers reverse to chronological order.
     */
    List<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

}