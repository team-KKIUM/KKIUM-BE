package com.kusitms.kkium.jd.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.kusitms.kkium.global.IntegrationTestBase;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

class JdEmbeddingRepositoryIntegrationTest extends IntegrationTestBase {

  private static final int EMBEDDING_DIMENSION = 1536;

  @Autowired private JdEmbeddingRepository jdEmbeddingRepository;

  @Autowired private JdRepository jdRepository;

  @Autowired private JdQuestionRepository jdQuestionRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  @DisplayName("공고 embedding vector를 실제 pgvector 컬럼에 저장한다")
  void saveJdEmbeddingVector() {
    Jd jd = saveJd();
    float[] embedding = embedding();

    jdEmbeddingRepository.saveEmbedding(jd.getId(), embedding);

    Integer dimensions =
        jdbcTemplate.queryForObject(
            "SELECT vector_dims(embedding) FROM jds WHERE id = ?", Integer.class, jd.getId());
    String savedEmbedding =
        jdbcTemplate.queryForObject(
            "SELECT embedding::text FROM jds WHERE id = ?", String.class, jd.getId());

    assertThat(dimensions).isEqualTo(EMBEDDING_DIMENSION);
    assertThat(savedEmbedding).startsWith("[0.1,0.2,0.3");
  }

  @Test
  @DisplayName("자기소개서 문항 embedding vector를 실제 pgvector 컬럼에 저장한다")
  void saveQuestionEmbeddingVector() {
    Jd jd = saveJd();
    JdQuestion question =
        jdQuestionRepository.saveAndFlush(
            JdQuestion.builder().jd(jd).orderNum(1).content("지원 동기를 작성해주세요.").build());
    float[] embedding = embedding();

    jdEmbeddingRepository.saveQuestionEmbedding(question.getId(), embedding);

    Integer dimensions =
        jdbcTemplate.queryForObject(
            "SELECT vector_dims(embedding) FROM jd_questions WHERE id = ?",
            Integer.class,
            question.getId());
    String savedEmbedding =
        jdbcTemplate.queryForObject(
            "SELECT embedding::text FROM jd_questions WHERE id = ?",
            String.class,
            question.getId());

    assertThat(dimensions).isEqualTo(EMBEDDING_DIMENSION);
    assertThat(savedEmbedding).startsWith("[0.1,0.2,0.3");
  }

  private Jd saveJd() {
    User user =
        userRepository.saveAndFlush(
            User.basicLoginBuilder()
                .name("테스트 유저")
                .email("jd-embedding-test-" + UUID.randomUUID() + "@example.com")
                .password("encoded-password")
                .build());

    return jdRepository.saveAndFlush(
        Jd.builder()
            .user(user)
            .postingTitle("백엔드 개발자")
            .companyName("끼움")
            .recruitmentField("백엔드")
            .rawText("공고 본문")
            .build());
  }

  private float[] embedding() {
    float[] embedding = new float[EMBEDDING_DIMENSION];
    embedding[0] = 0.1f;
    embedding[1] = 0.2f;
    embedding[2] = 0.3f;
    return embedding;
  }
}
