package com.springrag.skill.web;

import com.springrag.skill.dto.UserSkillResponse;
import com.springrag.skill.service.SkillTrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user-skills")
public class UserSkillController {

    private static final Logger log = LoggerFactory.getLogger(UserSkillController.class);

    private final SkillTrackingService skillTrackingService;

    public UserSkillController(SkillTrackingService skillTrackingService) {
        this.skillTrackingService = skillTrackingService;
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("@access.isCurrentUser(#userId) or hasRole('ADMIN')")
    public List<UserSkillResponse> getUserSkills(@PathVariable UUID userId) {
        log.info("Fetching skills for userId={}", userId);
        return skillTrackingService.getUserSkills(userId);
    }
}
