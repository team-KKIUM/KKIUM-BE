package com.kusitms.kkium.experience.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kusitms.kkium.experience.domain.Education;

public interface EducationRepository extends JpaRepository<Education, Long> {
  Optional<Education> findByExperienceId(Long experienceId);
}
