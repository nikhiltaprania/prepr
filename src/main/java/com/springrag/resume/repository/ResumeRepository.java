package com.springrag.resume.repository;

import com.springrag.resume.domain.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    Optional<Resume> findFirstByUserIdOrderByUploadedAtDesc(UUID userId);
}
