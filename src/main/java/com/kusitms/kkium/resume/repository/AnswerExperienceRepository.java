package com.kusitms.kkium.resume.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kusitms.kkium.resume.domain.AnswerExperience;

public interface AnswerExperienceRepository extends JpaRepository<AnswerExperience, Long> {

  List<AnswerExperience> findByJdAnswerId(Long jdAnswerId);

  void deleteByJdAnswerId(Long jdAnswerId);
}
