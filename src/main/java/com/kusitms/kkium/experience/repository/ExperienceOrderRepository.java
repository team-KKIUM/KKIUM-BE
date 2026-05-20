package com.kusitms.kkium.experience.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kusitms.kkium.experience.domain.ExperienceOrder;
import com.kusitms.kkium.experience.domain.type.PieceType;

public interface ExperienceOrderRepository extends JpaRepository<ExperienceOrder, Long> {

  List<ExperienceOrder> findAllByUserIdAndPieceTypeAndExperienceIdIn(
      Long userId, PieceType pieceType, List<Long> experienceIds);

  @Query(
      "SELECT COALESCE(MAX(eo.sortOrder), 0) FROM ExperienceOrder eo WHERE eo.user.id = :userId AND eo.pieceType = :pieceType")
  int findMaxSortOrderByUserIdAndPieceType(
      @Param("userId") Long userId, @Param("pieceType") PieceType pieceType);

  void deleteAllByExperienceId(Long experienceId);
}
