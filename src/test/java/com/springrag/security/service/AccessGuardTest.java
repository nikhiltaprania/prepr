package com.springrag.security.service;

import com.springrag.conversation.repository.ConversationRepository;
import com.springrag.interview.repository.InterviewQuestionRepository;
import com.springrag.interview.repository.InterviewSessionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class AccessGuardTest {

    @Mock
    private InterviewSessionRepository interviewRepository;
    @Mock
    private InterviewQuestionRepository questionRepository;
    @Mock
    private ConversationRepository conversationRepository;

    private AccessGuard accessGuard() {
        return new AccessGuard(interviewRepository, questionRepository, conversationRepository);
    }

    private void authenticateAs(UUID userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void isCurrentUserTrueOnlyForMatchingPrincipal() {
        UUID me = UUID.randomUUID();
        authenticateAs(me);
        AccessGuard guard = accessGuard();

        assertThat(guard.isCurrentUser(me)).isTrue();
        assertThat(guard.isCurrentUser(UUID.randomUUID())).isFalse();
        assertThat(guard.isCurrentUser(null)).isFalse();
    }

    @Test
    void ownsInterviewComparesOwnerToPrincipal() {
        UUID me = UUID.randomUUID();
        UUID interviewId = UUID.randomUUID();
        authenticateAs(me);
        lenient().when(interviewRepository.findOwnerId(interviewId)).thenReturn(Optional.of(me));

        assertThat(accessGuard().ownsInterview(interviewId)).isTrue();
    }

    @Test
    void ownsInterviewFalseForOtherOwnerOrMissing() {
        UUID me = UUID.randomUUID();
        UUID interviewId = UUID.randomUUID();
        authenticateAs(me);
        lenient().when(interviewRepository.findOwnerId(interviewId)).thenReturn(Optional.of(UUID.randomUUID()));

        assertThat(accessGuard().ownsInterview(interviewId)).isFalse();
        assertThat(accessGuard().ownsInterview(UUID.randomUUID())).isFalse();
    }

    @Test
    void ownsConversationAllowsNullButRejectsForeign() {
        UUID me = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        authenticateAs(me);
        lenient().when(conversationRepository.findOwnerId(conversationId)).thenReturn(Optional.of(UUID.randomUUID()));

        assertThat(accessGuard().ownsConversation(null)).isTrue();           // no conversation supplied
        assertThat(accessGuard().ownsConversation(conversationId)).isFalse();// belongs to someone else
    }

    @Test
    void unauthenticatedHasNoAccess() {
        AccessGuard guard = accessGuard();
        assertThat(guard.currentUserId()).isNull();
        assertThat(guard.isCurrentUser(UUID.randomUUID())).isFalse();
    }
}
