package com.kusitms.kkium.experience.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.kusitms.kkium.experience.domain.Piece;
import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.global.IntegrationTestBase;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

class PieceEmbeddingRepositoryIntegrationTest extends IntegrationTestBase {

  private static final int EMBEDDING_DIMENSION = 1536;

  @Autowired private PieceEmbeddingRepository pieceEmbeddingRepository;

  @Autowired private PieceRepository pieceRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("경험 embedding vector를 실제 pgvector 컬럼에 저장한다")
  void savePieceEmbeddingVector() {
    Piece piece = savePiece();
    float[] embedding = embedding();

    pieceEmbeddingRepository.saveEmbedding(piece.getId(), embedding);

    Integer dimensions =
        jdbcTemplate.queryForObject(
            "SELECT vector_dims(embedding) FROM pieces WHERE id = ?", Integer.class, piece.getId());
    String savedEmbedding =
        jdbcTemplate.queryForObject(
            "SELECT embedding::text FROM pieces WHERE id = ?", String.class, piece.getId());

    assertThat(dimensions).isEqualTo(EMBEDDING_DIMENSION);
    assertThat(savedEmbedding).startsWith("[0.1,0.2,0.3");
  }

  private Piece savePiece() {
    User user =
        userRepository.saveAndFlush(
            User.basicLoginBuilder()
                .name("테스트 유저")
                .email("piece-embedding-test-" + UUID.randomUUID() + "@example.com")
                .password("encoded-password")
                .build());

    return pieceRepository.saveAndFlush(
        Piece.builder().type(PieceType.ACTIVITY).user(user).build());
  }

  private float[] embedding() {
    float[] embedding = new float[EMBEDDING_DIMENSION];
    embedding[0] = 0.1f;
    embedding[1] = 0.2f;
    embedding[2] = 0.3f;
    return embedding;
  }
}
