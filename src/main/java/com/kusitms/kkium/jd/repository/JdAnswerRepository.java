package com.kusitms.kkium.jd.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kusitms.kkium.jd.domain.JdAnswer;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.user.domain.User;

public interface JdAnswerRepository extends JpaRepository<JdAnswer, Long> {
  Optional<JdAnswer> findByJdQuestionAndUser(JdQuestion jdQuestion, User user);

  List<JdAnswer> findAllByJdQuestionInAndUser(List<JdQuestion> questions, User user);

  @Modifying
  @Query("DELETE FROM JdAnswer a WHERE a.jdQuestion = :jdQuestion")
  void deleteAllByJdQuestion(@Param("jdQuestion") JdQuestion jdQuestion);
}
