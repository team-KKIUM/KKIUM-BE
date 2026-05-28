package com.kusitms.kkium.home.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kusitms.kkium.experience.domain.Piece;
import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.jd.domain.Jd;

public interface HomeRepository extends JpaRepository<Piece, Long> {

  // 전체 경험 수
  @Query("SELECT COUNT(p) FROM Piece p WHERE p.user.id = :userId AND p.deleteAt IS NULL")
  int countTotalExperience(@Param("userId") Long userId);

  // 이번 달 경험 수
  @Query(
      "SELECT COUNT(e) FROM Experience e JOIN e.piece p "
          + "WHERE p.user.id = :userId AND p.deleteAt IS NULL "
          + "AND e.createdDate >= :start AND e.createdDate < :end")
  int countThisMonthExperience(
      @Param("userId") Long userId,
      @Param("start") LocalDateTime start,
      @Param("end") LocalDateTime end);

  // 경험 분포 (type별 COUNT)
  @Query(
      "SELECT p.type, COUNT(p) FROM Piece p "
          + "WHERE p.user.id = :userId AND p.deleteAt IS NULL "
          + "AND p.type != :allType "
          + "GROUP BY p.type")
  List<Object[]> countByPieceType(
      @Param("userId") Long userId, @Param("allType") PieceType allType);

  // 목표 공고 조회
  @Query(
      "SELECT j FROM Jd j WHERE j.user.id = :userId AND j.isTarget = true AND j.deleteAt IS NULL")
  List<Jd> findTargetJds(@Param("userId") Long userId);
}
