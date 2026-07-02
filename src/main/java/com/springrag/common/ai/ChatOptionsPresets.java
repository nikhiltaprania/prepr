package com.springrag.common.ai;

import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * Shared {@link ChatOptions} presets so scoring/structured AI calls don't each hard-code decoding
 * parameters. The application default temperature (0.7, in {@code application.yml}) suits free-form
 * conversational turns (interviewer, chat). Anything that must be repeatable — answer evaluation,
 * report generation, rerank, memory extraction — should decode deterministically so the same input
 * yields the same score/output run-to-run.
 */
public final class ChatOptionsPresets {

    private ChatOptionsPresets() {
    }

    /**
     * Temperature 0 for repeatable scoring/structured extraction. Returns a fresh builder per call
     * because {@code ChatClient.options(..)} consumes a {@link ChatOptions.Builder}.
     */
    public static ChatOptions.Builder<?> deterministic() {
        return ChatOptions.builder().temperature(0.0);
    }
}
