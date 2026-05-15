package com.kusitms.kkium.experience.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kusitms.kkium.experience.domain.Career;

public interface CareerRepository extends JpaRepository<Career, Long> {
  Optional<Career> findByExperienceId(Long experienceId);
}
