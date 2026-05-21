package com.kusitms.kkium.jd.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdQuestion;

public interface JdQuestionRepository extends JpaRepository<JdQuestion, Long> {
  List<JdQuestion> findByJdOrderByOrderNum(Jd jd);

  List<JdQuestion> findAllByIdInAndJdId(List<Long> ids, Long jdId);
}
