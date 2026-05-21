package com.kusitms.kkium.experience.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kusitms.kkium.experience.domain.Piece;

public interface PieceRepository extends JpaRepository<Piece, Long> {
  List<Piece> findByUserId(Long userId);

  Optional<Piece> findByIdAndDeleteAtIsNull(Long id);
}
