package com.springrag.memory.dto;

import com.springrag.memory.domain.MemoryCategory;

public class ExtractedMemory {

    private MemoryCategory category;

    private String content;

    public ExtractedMemory() {
    }

    public MemoryCategory getCategory() {
        return category;
    }

    public void setCategory(
            MemoryCategory category) {

        this.category = category;
    }

    public String getContent() {
        return content;
    }

    public void setContent(
            String content) {

        this.content = content;
    }
}