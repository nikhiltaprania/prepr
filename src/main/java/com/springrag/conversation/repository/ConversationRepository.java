package com.springrag.conversation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
import com.springrag.conversation.domain.Conversation;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /** Owner id only — used for ownership checks without loading the entity or its lazy User. */
    @Query("select c.user.id from Conversation c where c.id = :conversationId")
    Optional<UUID> findOwnerId(@Param("conversationId") UUID conversationId);
}