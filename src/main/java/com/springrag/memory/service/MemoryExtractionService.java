package com.springrag.memory.service;

import java.util.List;
import com.springrag.memory.dto.ExtractedMemory;

public interface MemoryExtractionService {

    List<ExtractedMemory>
    extractMemories(
            String message);
}