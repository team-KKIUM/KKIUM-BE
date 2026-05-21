package com.kusitms.kkium.resume.repository;

import com.kusitms.kkium.resume.domain.AnswerExperience;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerExperienceRepository extends JpaRepository<AnswerExperience, Long> {

  List<AnswerExperience> findByJdAnswerId(Long jdAnswerId);

  void deleteByJdAnswerId(Long jdAnswerId);
}
