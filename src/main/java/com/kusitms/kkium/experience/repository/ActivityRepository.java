package com.kusitms.kkium.experience.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kusitms.kkium.experience.domain.Activity;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
  Optional<Activity> findByExperienceId(Long experienceId);

  List<Activity> findByExperienceIdIn(List<Long> experienceIds);
}
