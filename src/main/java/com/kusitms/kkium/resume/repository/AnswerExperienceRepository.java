package com.kusitms.kkium.resume.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kusitms.kkium.resume.domain.AnswerExperience;

public interface AnswerExperienceRepository extends JpaRepository<AnswerExperience, Long> {

  List<AnswerExperience> findByJdAnswerId(Long jdAnswerId);

  @Modifying
  @Query("DELETE FROM AnswerExperience ae WHERE ae.jdAnswer.id IN :answerIds")
  void deleteAllByJdAnswerIdIn(@Param("answerIds") List<Long> answerIds);
}
