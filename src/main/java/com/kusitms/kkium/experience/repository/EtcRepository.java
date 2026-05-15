package com.kusitms.kkium.experience.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kusitms.kkium.experience.domain.Etc;

public interface EtcRepository extends JpaRepository<Etc, Long> {
  Optional<Etc> findByExperienceId(Long experienceId);

  List<Etc> findByExperienceIdIn(List<Long> experienceIds);
}
