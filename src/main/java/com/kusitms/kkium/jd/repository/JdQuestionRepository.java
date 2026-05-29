package com.kusitms.kkium.jd.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdQuestion;

public interface JdQuestionRepository extends JpaRepository<JdQuestion, Long> {
  List<JdQuestion> findByJdOrderByOrderNum(Jd jd);

  List<JdQuestion> findAllByIdInAndJdId(List<Long> ids, Long jdId);

  @Query("SELECT COALESCE(MAX(q.orderNum), 0) FROM JdQuestion q WHERE q.jd.id = :jdId")
  int findMaxOrderNumByJdId(@Param("jdId") Long jdId);
}
