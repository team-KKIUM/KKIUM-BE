package com.kusitms.kkium.experience.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.domain.type.PieceType;

public interface ExperienceRepository extends JpaRepository<Experience, Long> {

  // 전체 조회 (cursor 없음, 첫 페이지)
  @Query(
      "SELECT e FROM Experience e JOIN FETCH e.piece p WHERE p.user.id = :userId "
          + "ORDER BY e.id DESC")
  List<Experience> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

  // 전체 조회 (cursor 있음)
  @Query(
      "SELECT e FROM Experience e JOIN FETCH e.piece p WHERE p.user.id = :userId "
          + "AND e.id < :cursor "
          + "ORDER BY e.id DESC")
  List<Experience> findAllByUserIdAndCursor(
      @Param("userId") Long userId, @Param("cursor") Long cursor, Pageable pageable);

  // type 필터 조회 (cursor 없음, 첫 페이지)
  @Query(
      "SELECT e FROM Experience e JOIN FETCH e.piece p WHERE p.user.id = :userId "
          + "AND p.type = :type "
          + "ORDER BY e.id DESC")
  List<Experience> findAllByUserIdAndType(
      @Param("userId") Long userId, @Param("type") PieceType type, Pageable pageable);

  // type 필터 조회 (cursor 있음)
  @Query(
      "SELECT e FROM Experience e JOIN FETCH e.piece p WHERE p.user.id = :userId "
          + "AND p.type = :type "
          + "AND e.id < :cursor "
          + "ORDER BY e.id DESC")
  List<Experience> findAllByUserIdAndTypeAndCursor(
      @Param("userId") Long userId,
      @Param("type") PieceType type,
      @Param("cursor") Long cursor,
      Pageable pageable);
}
