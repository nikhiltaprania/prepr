package com.springrag.interview.service.impl;

import com.springrag.common.exception.ResourceNotFoundException;
import com.springrag.conversation.domain.Conversation;
import com.springrag.conversation.domain.Message;
import com.springrag.conversation.domain.MessageRole;
import com.springrag.conversation.service.ConversationService;
import com.springrag.conversation.service.MessageService;
import com.springrag.interview.domain.InterviewQuestion;
import com.springrag.interview.domain.InterviewSession;
import com.springrag.interview.domain.TranscriptSpeaker;
import com.springrag.interview.event.InterviewCompletedEvent;
import com.springrag.interview.dto.ConversationTurnResponse;
import com.springrag.interview.dto.CreateInterviewAnswerRequest;
import com.springrag.interview.dto.InterviewQuestionResponse;
import com.springrag.interview.dto.InterviewerTurn;
import com.springrag.interview.dto.TranscriptEntryResponse;
import com.springrag.interview.repository.InterviewAnswerRepository;
import com.springrag.interview.repository.InterviewQuestionRepository;
import com.springrag.interview.repository.InterviewSessionRepository;
import com.springrag.interview.service.InterviewAnswerService;
import com.springrag.interview.service.InterviewConversationService;
import com.springrag.interview.service.InterviewQuestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Conducts the interview as a natural, human-like conversation (the voice/text channel). The model
 * acts as a real interviewer: it greets and asks for an introduction at the start, acknowledges
 * answers, probes or clarifies when useful, and only advances to the next PLANNED question when it
 * decides the current answer is sufficient — so it never blindly marches through a questionnaire.
 *
 * Reuses the shared interview execution (planned-question order via {@link InterviewQuestionService},
 * answer recording + evaluation via {@link InterviewAnswerService}) and the conversation-memory module
 * for turn-by-turn awareness. The structured questions/answers remain the source of truth for scoring.
 */
@Service
public class InterviewConversationServiceImpl implements InterviewConversationService {

    private static final Logger log = LoggerFactory.getLogger(InterviewConversationServiceImpl.class);
    private static final String CLOSING = "Thank you, that concludes the interview. It was a pleasure speaking with you.";
    private static final int HISTORY_TURNS = 12;
    /** Bounded history window loaded per turn: covers the prompt window (HISTORY_TURNS) plus the
     *  current answer's messages, without scanning the whole conversation. */
    private static final int HISTORY_WINDOW = 30;

    private static final String INSTRUCTIONS = """
            You are conducting a live, spoken job interview. Behave like a real, warm, professional
            interviewer — never like a questionnaire.

            Guidelines:
            - If the interview has not started yet (no questions asked and no prior conversation), greet the
              candidate, briefly introduce yourself and the interview, and ask them to introduce themselves.
              Do NOT ask a technical question yet.
            - After the candidate introduces themselves, acknowledge it warmly and transition into the interview.
            - When the candidate answers a question, respond naturally: acknowledge or give brief encouragement.
              If the answer is incomplete, unclear, or interesting, ask ONE follow-up or clarifying question and
              stay on the topic. Do not over-interrogate — at most a couple of follow-ups per question.
            - When the candidate has answered sufficiently, transition smoothly and ask the NEXT PLANNED QUESTION
              below, phrased in your own natural words.
            - If there are no more planned questions, wrap up the interview professionally with a brief, positive close.
            - Keep each turn concise and conversational, and ask at most ONE question per turn.
            """;

    private final ChatClient chatClient;
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final InterviewSessionRepository interviewRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final InterviewQuestionService questionService;
    private final InterviewAnswerService answerService;
    private final ApplicationEventPublisher eventPublisher;

    public InterviewConversationServiceImpl(ChatClient.Builder chatClientBuilder,
                                            ConversationService conversationService, MessageService messageService,
                                            InterviewSessionRepository interviewRepository,
                                            InterviewQuestionRepository questionRepository,
                                            InterviewAnswerRepository answerRepository,
                                            InterviewQuestionService questionService,
                                            InterviewAnswerService answerService,
                                            ApplicationEventPublisher eventPublisher) {
        this.chatClient = chatClientBuilder.build();
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.interviewRepository = interviewRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.questionService = questionService;
        this.answerService = answerService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ConversationTurnResponse respond(UUID interviewId, String candidateText) {
        TurnState state = loadState(interviewId);
        InterviewerTurn turn = runTurn(state, candidateText);
        String reply = finalizeReply(turn.getReply());
        boolean completed = applyTurn(state, candidateText, reply, turn.isMoveToNextQuestion());
        return new ConversationTurnResponse(interviewId, reply, completed);
    }

    @Override
    public List<TranscriptEntryResponse> getTranscript(UUID interviewId) {
        InterviewSession interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + interviewId));
        if (interview.getConversationId() == null) {
            return List.of();
        }
        return messageService.getMessages(interview.getConversationId()).stream()
                .map(message -> new TranscriptEntryResponse(
                        message.getRole() == MessageRole.ASSISTANT ? TranscriptSpeaker.AI : TranscriptSpeaker.CANDIDATE,
                        message.getContent()))
                .toList();
    }

    /** Loads everything a turn needs (interview, conversation, history, current + next question). */
    private TurnState loadState(UUID interviewId) {
        InterviewSession interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + interviewId));
        Conversation conversation = ensureConversation(interview);
        List<Message> history = messageService.getRecentMessages(conversation.getId(), HISTORY_WINDOW);
        InterviewQuestion current = questionRepository
                .findFirstByInterviewIdAndAskedAtIsNotNullOrderBySequenceNumberDesc(interviewId).orElse(null);
        boolean currentAnswered = current != null && answerRepository.existsByQuestionId(current.getId());
        Optional<InterviewQuestionResponse> nextPlanned = questionService.peekNextQuestion(interviewId);
        return new TurnState(interview, conversation, history, current, currentAnswered, nextPlanned);
    }

    /** Persists the turn, records/evaluates the answer when moving on, and advances the question. */
    private boolean applyTurn(TurnState state, String candidateText, String reply, boolean moveOn) {
        if (candidateText != null && !candidateText.isBlank()) {
            messageService.saveUserMessage(state.conversation, candidateText);
        }
        if (moveOn && state.current != null && !state.currentAnswered) {
            String answer = accumulateAnswer(state.history, state.current.getAskedAt(), candidateText);
            if (!answer.isBlank()) {
                CreateInterviewAnswerRequest request = new CreateInterviewAnswerRequest();
                request.setQuestionId(state.current.getId());
                request.setAnswer(answer);
                answerService.create(request);
            }
        }
        boolean completed = false;
        if (moveOn) {
            if (state.nextPlanned.isPresent()) {
                questionService.markAsked(state.nextPlanned.get().getId());
            } else {
                completed = true;
            }
        }
        messageService.saveAssistantMessage(state.conversation, reply);
        log.info("Interview turn. interviewId={}, moveOn={}, completed={}", state.interview.getId(), moveOn, completed);

        if (completed) {
            // Decoupled hand-off: the report module listens and generates the report, then triggers
            // skill aggregation. No compile-time dependency from interview -> report/skill.
            eventPublisher.publishEvent(new InterviewCompletedEvent(state.interview.getId()));
            log.info("Interview completed; published InterviewCompletedEvent. interviewId={}", state.interview.getId());
        }
        return completed;
    }

    private InterviewerTurn runTurn(TurnState state, String candidateText) {
        try {
            InterviewerTurn turn = chatClient.prompt(buildStructuredPrompt(state, candidateText))
                    .call().entity(InterviewerTurn.class);
            if (turn != null && turn.getReply() != null && !turn.getReply().isBlank()) {
                return turn;
            }
        } catch (Exception ex) {
            log.warn("Interviewer turn generation failed; falling back. interviewId={}, error={}",
                    state.interview.getId(), ex.getMessage());
        }
        InterviewerTurn fallback = new InterviewerTurn();
        fallback.setMoveToNextQuestion(true);
        fallback.setReply(state.nextPlanned.map(InterviewQuestionResponse::getQuestion).orElse(CLOSING));
        return fallback;
    }

    private String buildStructuredPrompt(TurnState state, String candidateText) {
        return INSTRUCTIONS + """

                Set moveToNextQuestion = true ONLY when your reply asks/transitions to the next planned question
                (including the first transition from the candidate's introduction to the first question). Set it to
                false when you greet, ask for an introduction, or ask a follow-up/clarification on the current topic.

                """ + buildContext(state, candidateText);
    }

    private String buildContext(TurnState state, String candidateText) {
        String currentText = state.current == null ? "none (the interview has not started yet)"
                : state.current.getQuestion() + (state.currentAnswered ? " (already answered)" : "");
        String nextText = state.nextPlanned.map(InterviewQuestionResponse::getQuestion)
                .orElse("none — there are no more planned questions, so wrap up the interview");

        return """
                Interview type: %s
                Difficulty: %s

                Current question under discussion: %s
                Next planned question to move to when ready: %s

                %s

                Candidate's latest message: %s
                """.formatted(state.interview.getType(), state.interview.getDifficulty(), currentText, nextText,
                formatHistory(state.history), candidateText == null || candidateText.isBlank()
                        ? "(the candidate is ready to begin)" : candidateText);
    }

    private String formatHistory(List<Message> history) {
        if (history.isEmpty()) {
            return "Conversation so far: (this is the very start of the interview)";
        }
        StringBuilder sb = new StringBuilder("Conversation so far:\n");
        int from = Math.max(0, history.size() - HISTORY_TURNS);
        for (Message message : history.subList(from, history.size())) {
            sb.append(message.getRole() == MessageRole.ASSISTANT ? "Interviewer: " : "Candidate: ")
                    .append(message.getContent()).append("\n");
        }
        return sb.toString();
    }

    /** The candidate's full answer to the current question: their messages since it was asked, plus this turn's. */
    private String accumulateAnswer(List<Message> history, LocalDateTime askedAt, String candidateText) {
        StringBuilder answer = new StringBuilder();
        if (askedAt != null) {
            for (Message message : history) {
                if (message.getRole() == MessageRole.USER && message.getCreatedAt() != null
                        && !message.getCreatedAt().isBefore(askedAt)) {
                    answer.append(message.getContent()).append(" ");
                }
            }
        }
        if (candidateText != null && !candidateText.isBlank()) {
            answer.append(candidateText);
        }
        return answer.toString().strip();
    }

    private String finalizeReply(String reply) {
        return (reply == null || reply.isBlank()) ? CLOSING : reply.strip();
    }

    private Conversation ensureConversation(InterviewSession interview) {
        if (interview.getConversationId() != null) {
            return conversationService.getConversation(interview.getConversationId());
        }
        Conversation conversation = conversationService.createConversation(
                interview.getUser().getId(), "Interview " + interview.getType());
        interview.setConversationId(conversation.getId());
        interviewRepository.save(interview);
        return conversation;
    }

    /** Immutable working state for a single interview turn. */
    private static final class TurnState {
        private final InterviewSession interview;
        private final Conversation conversation;
        private final List<Message> history;
        private final InterviewQuestion current;
        private final boolean currentAnswered;
        private final Optional<InterviewQuestionResponse> nextPlanned;

        private TurnState(InterviewSession interview, Conversation conversation, List<Message> history,
                          InterviewQuestion current, boolean currentAnswered,
                          Optional<InterviewQuestionResponse> nextPlanned) {
            this.interview = interview;
            this.conversation = conversation;
            this.history = history;
            this.current = current;
            this.currentAnswered = currentAnswered;
            this.nextPlanned = nextPlanned;
        }
    }
}
