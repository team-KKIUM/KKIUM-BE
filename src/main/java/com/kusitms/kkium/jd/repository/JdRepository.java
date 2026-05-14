package com.kusitms.kkium.jd.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kusitms.kkium.jd.domain.Jd;

public interface JdRepository extends JpaRepository<Jd, Long> {

  @Query(
      "SELECT j FROM Jd j WHERE j.user.id = :userId AND j.deleteAt IS NULL AND (:keyword IS NULL OR j.postingTitle LIKE %:keyword% OR j.companyName LIKE %:keyword% OR j.recruitmentField LIKE %:keyword%) ORDER BY j.sortOrder ASC NULLS LAST, j.createdDate DESC")
  Page<Jd> findByUserIdAndDeleteAtIsNull(
      @Param("userId") Long userId, @Param("keyword") String keyword, Pageable pageable);

  Optional<Jd> findByIdAndDeleteAtIsNull(Long id);

  long countByUserIdAndIsTargetTrueAndDeleteAtIsNull(Long userId);

  List<Jd> findAllByIdInAndDeleteAtIsNull(List<Long> ids);
}
