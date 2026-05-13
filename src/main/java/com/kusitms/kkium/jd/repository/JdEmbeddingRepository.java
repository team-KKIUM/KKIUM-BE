package com.kusitms.kkium.jd.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.pgvector.PGvector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class JdEmbeddingRepository {

  private final JdbcTemplate jdbcTemplate;

  public void saveEmbedding(Long jdId, float[] embedding) {
    try {
      PGvector vector = new PGvector(embedding);
      jdbcTemplate.update("UPDATE jds SET embedding = ? WHERE id = ?", vector, jdId);
    } catch (Exception e) {
      log.warn("임베딩 저장 실패 - jdId: {}, 원인: {}", jdId, e.getMessage());
    }
  }
}
