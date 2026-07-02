package com.springrag.skill.service;

import com.springrag.skill.dto.UserSkillResponse;

import java.util.List;
import java.util.UUID;

public interface SkillTrackingService {

    void updateSkills(UUID interviewId);

    List<UserSkillResponse> getUserSkills(UUID userId);
}
