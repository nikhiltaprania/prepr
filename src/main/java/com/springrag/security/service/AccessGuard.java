package com.springrag.security.service;

import com.springrag.conversation.repository.ConversationRepository;
import com.springrag.interview.repository.InterviewQuestionRepository;
import com.springrag.interview.repository.InterviewSessionRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Ownership / authorization helper exposed to SpEL as {@code @access} for use in
 * {@code @PreAuthorize}. Centralizes "does the authenticated principal own this resource?" so
 * controllers stay declarative, e.g.:
 *
 * <pre>{@code @PreAuthorize("@access.isCurrentUser(#userId) or hasRole('ADMIN')")}</pre>
 *
 * The principal's name is the user's UUID (the JWT subject). Owner lookups use id-only projections,
 * so they run safely outside a transaction (no lazy initialization).
 */
@Component("access")
public class AccessGuard {

    private final InterviewSessionRepository interviewRepository;
    private final InterviewQuestionRepository questionRepository;
    private final ConversationRepository conversationRepository;

    public AccessGuard(InterviewSessionRepository interviewRepository,
                       InterviewQuestionRepository questionRepository,
                       ConversationRepository conversationRepository) {
        this.interviewRepository = interviewRepository;
        this.questionRepository = questionRepository;
        this.conversationRepository = conversationRepository;
    }

    /** True when the path/body user id matches the authenticated principal. */
    public boolean isCurrentUser(UUID userId) {
        return userId != null && userId.toString().equals(currentUserId());
    }

    /** True when the interview belongs to the authenticated principal. */
    public boolean ownsInterview(UUID interviewId) {
        if (interviewId == null) {
            return false;
        }
        return interviewRepository.findOwnerId(interviewId)
                .map(owner -> owner.toString().equals(currentUserId()))
                .orElse(false);
    }

    /** True when the question's interview belongs to the authenticated principal. */
    public boolean ownsQuestion(UUID questionId) {
        if (questionId == null) {
            return false;
        }
        return questionRepository.findOwnerId(questionId)
                .map(owner -> owner.toString().equals(currentUserId()))
                .orElse(false);
    }

    /**
     * True when the conversation belongs to the principal. A {@code null} id means "no conversation
     * supplied" (some endpoints allow anonymous, conversation-less calls) and is permitted.
     */
    public boolean ownsConversation(UUID conversationId) {
        if (conversationId == null) {
            return true;
        }
        return conversationRepository.findOwnerId(conversationId)
                .map(owner -> owner.toString().equals(currentUserId()))
                .orElse(false);
    }

    /** The authenticated user's id, or {@code null} if unauthenticated. */
    public String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() ? authentication.getName() : null;
    }
}
