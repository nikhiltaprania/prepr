package com.springrag.skill.repository;

import com.springrag.skill.domain.SkillCategory;
import com.springrag.skill.domain.UserSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSkillRepository extends JpaRepository<UserSkill, UUID> {

    Optional<UserSkill> findByUserIdAndSkillCategory(UUID userId, SkillCategory category);

    List<UserSkill> findByUserIdOrderByAverageScoreDesc(UUID userId);
}
