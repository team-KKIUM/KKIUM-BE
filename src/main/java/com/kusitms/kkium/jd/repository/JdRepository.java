package com.kusitms.kkium.jd.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.kusitms.kkium.jd.domain.Jd;

public interface JdRepository extends JpaRepository<Jd, Long> {

  Page<Jd> findByUserIdAndDeleteAtIsNull(Long userId, Pageable pageable);

  boolean existsByUserIdAndDeleteAtIsNullAndSortOrderIsNotNull(Long userId);

  Optional<Jd> findByIdAndDeleteAtIsNull(Long id);

  long countByUserIdAndIsTargetTrueAndDeleteAtIsNull(Long userId);
}
