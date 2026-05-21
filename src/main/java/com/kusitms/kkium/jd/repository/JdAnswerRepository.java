package com.kusitms.kkium.jd.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kusitms.kkium.jd.domain.JdAnswer;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.user.domain.User;

public interface JdAnswerRepository extends JpaRepository<JdAnswer, Long> {
  Optional<JdAnswer> findByJdQuestionAndUser(JdQuestion jdQuestion, User user);

  List<JdAnswer> findAllByJdQuestionInAndUser(List<JdQuestion> questions, User user);
}
