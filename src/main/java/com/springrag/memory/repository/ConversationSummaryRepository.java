package com.springrag.memory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
import com.springrag.memory.domain.ConversationSummary;

@Repository
public interface ConversationSummaryRepository extends JpaRepository<ConversationSummary, UUID> {

    Optional<ConversationSummary> findByConversationId(UUID conversationId);

}