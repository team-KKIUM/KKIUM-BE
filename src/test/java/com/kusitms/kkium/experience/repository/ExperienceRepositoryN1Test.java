package com.kusitms.kkium.experience.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.domain.ExperienceOrder;
import com.kusitms.kkium.experience.domain.Piece;
import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.global.IntegrationTestBase;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

class ExperienceRepositoryN1Test extends IntegrationTestBase {

  @Autowired private ExperienceRepository experienceRepository;
  @Autowired private PieceRepository pieceRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ExperienceOrderRepository experienceOrderRepository;
  @Autowired private EntityManagerFactory emf;

  @PersistenceContext private EntityManager em;

  private Statistics stats;

  @BeforeEach
  void setUp() {
    stats = emf.unwrap(SessionFactory.class).getStatistics();
    stats.setStatisticsEnabled(true);
  }

  @AfterEach
  void tearDown() {
    if (stats != null) {
      stats.setStatisticsEnabled(false);
    }
  }

  @Transactional
  @Test
  @DisplayName("findByIdWithPiece는 JOIN FETCH로 Piece를 함께 로드하여 추가 쿼리가 발생하지 않는다")
  void findByIdWithPiece_N1_없음() {
    // given
    User user =
        userRepository.save(
            User.basicLoginBuilder()
                .name("테스트")
                .email("n1test1@kkium.com")
                .password("password")
                .build());
    Piece piece = pieceRepository.save(Piece.builder().type(PieceType.ACTIVITY).user(user).build());
    Experience experience =
        experienceRepository.save(Experience.builder().title("경험 제목").piece(piece).build());

    em.flush();
    em.clear(); // 1차 캐시 비우기
    stats.clear();

    // when
    Experience found = experienceRepository.findByIdWithPiece(experience.getId()).orElseThrow();

    // then — Piece에 접근해도 추가 쿼리 없음 (JOIN FETCH로 이미 로드)
    assertThat(found.getPiece()).isNotNull();
    assertThat(found.getPiece().getType()).isEqualTo(PieceType.ACTIVITY);
    assertThat(stats.getPrepareStatementCount())
        .as("JOIN FETCH로 Experience + Piece를 단 1개 쿼리로 조회")
        .isEqualTo(1L);
  }

  @Transactional
  @Test
  @DisplayName("findAllByUserId는 N개 Experience 조회 시 Piece를 JOIN FETCH로 한 번에 로드하여 N+1 쿼리가 발생하지 않는다")
  void findAllByUserId_N1_없음() {
    // given — 3개 Experience 저장
    User user =
        userRepository.save(
            User.basicLoginBuilder()
                .name("유저")
                .email("n1test2@kkium.com")
                .password("password")
                .build());

    for (int i = 1; i <= 3; i++) {
      Piece piece =
          pieceRepository.save(Piece.builder().type(PieceType.ACTIVITY).user(user).build());
      Experience exp =
          experienceRepository.save(Experience.builder().title("경험" + i).piece(piece).build());
      experienceOrderRepository.save(
          ExperienceOrder.builder()
              .sortOrder(i)
              .pieceType(PieceType.ALL)
              .experience(exp)
              .user(user)
              .build());
    }

    em.flush();
    em.clear();
    stats.clear();

    // when
    List<Experience> experiences =
        experienceRepository.findAllByUserId(user.getId(), PageRequest.of(0, 10));

    // then — 3개 Experience의 Piece 접근해도 추가 쿼리 없음
    assertThat(experiences).hasSize(3);
    experiences.forEach(e -> assertThat(e.getPiece()).isNotNull());
    assertThat(stats.getPrepareStatementCount())
        .as("3개 Experience + Piece를 단 1개 쿼리로 조회 (N+1 없음)")
        .isEqualTo(1L);
  }
}
