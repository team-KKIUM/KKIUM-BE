package com.kusitms.kkium.experience.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kusitms.kkium.experience.domain.Experience;

public interface ExperienceRepository extends JpaRepository<Experience, Long> {}
