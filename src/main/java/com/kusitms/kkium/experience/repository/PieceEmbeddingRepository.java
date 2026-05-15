package com.kusitms.kkium.experience.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.pgvector.PGvector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class PieceEmbeddingRepository {

  private final JdbcTemplate jdbcTemplate;

  public void saveEmbedding(Long pieceId, float[] embedding) {
    try {
      PGvector vector = new PGvector(embedding);
      jdbcTemplate.update("UPDATE pieces SET embedding = ? WHERE id = ?", vector, pieceId);
    } catch (Exception e) {
      log.warn("Piece 임베딩 저장 실패 - pieceId: {}, 원인: {}", pieceId, e.getMessage());
    }
  }
}
