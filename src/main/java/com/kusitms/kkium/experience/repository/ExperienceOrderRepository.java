package com.kusitms.kkium.experience.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kusitms.kkium.experience.domain.ExperienceOrder;
import com.kusitms.kkium.experience.domain.type.PieceType;

public interface ExperienceOrderRepository extends JpaRepository<ExperienceOrder, Long> {

  List<ExperienceOrder> findAllByUserIdAndPieceTypeAndExperienceIdIn(
      Long userId, PieceType pieceType, List<Long> experienceIds);

  int countByUserIdAndPieceType(Long userId, PieceType pieceType);

  void deleteAllByExperienceId(Long experienceId);
}
