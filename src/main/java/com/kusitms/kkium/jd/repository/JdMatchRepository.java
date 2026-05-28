package com.kusitms.kkium.jd.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class JdMatchRepository {

  private final JdbcTemplate jdbcTemplate;

  /**
   * JD 임베딩 vs 유저의 전체 Piece 임베딩 코사인 유사도 계산 pgvector의 <=> 연산자는 코사인 거리(0~1)를 반환하므로 유사도 = 1 - distance
   * (0~1 범위) 이를 0~100으로 정규화: (1 - distance) * 100
   */
  public List<PieceSimilarity> findSimilaritiesByJdAndUser(Long jdId, Long userId) {
    String sql =
        """
        SELECT p.id AS piece_id,
               CAST(((1 - (p.embedding <=> j.embedding)) * 100) AS INTEGER) AS similarity_score
        FROM pieces p, jds j
        WHERE j.id = ?
          AND p.user_id = ?
          AND p.embedding IS NOT NULL
          AND j.embedding IS NOT NULL
        ORDER BY similarity_score DESC
        """;

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> new PieceSimilarity(rs.getLong("piece_id"), rs.getInt("similarity_score")),
        jdId,
        userId);
  }

  public record PieceSimilarity(Long pieceId, int similarityScore) {}
}
