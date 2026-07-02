package com.springrag.memory.service;

import java.util.UUID;

public interface SummaryService {

    String getSummary(UUID conversationId);

    void updateSummary(UUID conversationId);
    void regenerateSummary(UUID conversationId);

}