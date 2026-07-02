package com.springrag.memory.dto;

import java.util.UUID;

public class MemoryStatisticsResponse {

    private UUID userId;

    private long totalMemories;

    private long personal;

    private long preference;

    private long fact;

    private long project;

    private long goal;

    public MemoryStatisticsResponse() {
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public long getTotalMemories() {
        return totalMemories;
    }

    public void setTotalMemories(long totalMemories) {
        this.totalMemories = totalMemories;
    }

    public long getPersonal() {
        return personal;
    }

    public void setPersonal(long personal) {
        this.personal = personal;
    }

    public long getPreference() {
        return preference;
    }

    public void setPreference(long preference) {
        this.preference = preference;
    }

    public long getFact() {
        return fact;
    }

    public void setFact(long fact) {
        this.fact = fact;
    }

    public long getProject() {
        return project;
    }

    public void setProject(long project) {
        this.project = project;
    }

    public long getGoal() {
        return goal;
    }

    public void setGoal(long goal) {
        this.goal = goal;
    }
}