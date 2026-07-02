package com.springrag.interview.dto;

/**
 * The interviewer's decision for one conversation turn (Spring AI structured output): the natural
 * spoken reply, and whether this reply moves the interview on to the next planned question (vs.
 * greeting, asking for an introduction, or staying on the current topic with a follow-up).
 */
public class InterviewerTurn {

    private String reply;
    private boolean moveToNextQuestion;

    public InterviewerTurn() {
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public boolean isMoveToNextQuestion() {
        return moveToNextQuestion;
    }

    public void setMoveToNextQuestion(boolean moveToNextQuestion) {
        this.moveToNextQuestion = moveToNextQuestion;
    }
}
