package com.springrag.interview.event;

import java.util.UUID;

/**
 * Published when an interview's questions are exhausted and the dialogue has wrapped up. Decouples
 * the interview module from the downstream report/skill modules: the interview module never compiles
 * against {@code report} or {@code skill} (which both already depend on {@code interview}), so the
 * lifecycle is wired without a circular dependency.
 *
 * <p>Consumed by the report module, which generates the report and then updates skill aggregates.
 * In-process Spring application event — not event sourcing, just listener-based decoupling.
 */
public final class InterviewCompletedEvent {

    private final UUID interviewId;

    public InterviewCompletedEvent(UUID interviewId) {
        this.interviewId = interviewId;
    }

    public UUID getInterviewId() {
        return interviewId;
    }
}
