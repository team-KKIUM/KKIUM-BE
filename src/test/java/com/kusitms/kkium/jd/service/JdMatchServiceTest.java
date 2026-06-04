package com.kusitms.kkium.jd.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.JD_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.domain.Piece;
import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.experience.repository.ExperienceRepository;
import com.kusitms.kkium.experience.repository.TagRepository;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.type.AnalysisStatus;
import com.kusitms.kkium.jd.dto.response.JdMatchAnalysisResponse;
import com.kusitms.kkium.jd.repository.JdMatchRepository;
import com.kusitms.kkium.jd.repository.JdMatchRepository.PieceSimilarity;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.jd.utils.llm.LlmMatchScoreService;
import com.kusitms.kkium.jd.utils.llm.result.LlmMatchResult;
import com.kusitms.kkium.user.domain.User;

@ExtendWith(MockitoExtension.class)
class JdMatchServiceTest {

  @Mock private JdRepository jdRepository;
  @Mock private ExperienceRepository experienceRepository;
  @Mock private TagRepository tagRepository;
  @Mock private JdMatchRepository jdMatchRepository;
  @Mock private LlmMatchScoreService llmMatchScoreService;

  private JdMatchService jdMatchService;

  @BeforeEach
  void setUp() {
    jdMatchService =
        new JdMatchService(
            jdRepository,
            experienceRepository,
            tagRepository,
            jdMatchRepository,
            llmMatchScoreService);
  }

  @Test
  @DisplayName("존재하지 않는 JD이면 JD_NOT_FOUND 예외를 던진다")
  void throwWhenJdNotFound() {
    when(jdRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> jdMatchService.analyze(999L, 1L))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(JD_NOT_FOUND);
  }

  @Test
  @DisplayName("분석 상태가 PENDING이면 경험 조회 없이 상태만 반환한다")
  void returnPendingStatusWithoutExperienceQuery() {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createJd(owner, "Java, Spring", "협업");
    setId(jd, 10L);
    // Jd 빌더 기본값이 PENDING이므로 그대로 사용

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));

    JdMatchAnalysisResponse response = jdMatchService.analyze(10L, userId);

    assertThat(response.analysisStatus()).isEqualTo(AnalysisStatus.PENDING);
    assertThat(response.jdInfo()).isNull();
    assertThat(response.matchResult()).isNull();
    verify(experienceRepository, never()).findAllByUserIdForAnalysis(any());
  }

  @Test
  @DisplayName("분석 상태가 IN_PROGRESS이면 경험 조회 없이 상태만 반환한다")
  void returnInProgressStatusWithoutExperienceQuery() {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createJd(owner, "Java", "문제 해결");
    setId(jd, 10L);
    jd.updateAnalysisStatus(AnalysisStatus.PENDING);

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));

    JdMatchAnalysisResponse response = jdMatchService.analyze(10L, userId);

    assertThat(response.analysisStatus()).isEqualTo(AnalysisStatus.PENDING);
    assertThat(response.jdInfo()).isNull();
    verify(experienceRepository, never()).findAllByUserIdForAnalysis(any());
  }

  @Test
  @DisplayName("분석 완료 상태에서 유저 경험이 없으면 applicationFitScore=0, 빈 카드 목록을 반환한다")
  void returnEmptyMatchResultWhenNoExperiences() {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createCompletedJd(owner);
    setId(jd, 10L);

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(experienceRepository.findAllByUserIdForAnalysis(userId)).thenReturn(List.of());

    JdMatchAnalysisResponse response = jdMatchService.analyze(10L, userId);

    assertThat(response.analysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
    assertThat(response.jdInfo()).isNotNull();
    assertThat(response.matchResult().applicationFitScore()).isZero();
    assertThat(response.matchResult().experiences()).isEmpty();
    verify(llmMatchScoreService, never()).scoreAll(any(), any());
  }

  @Test
  @DisplayName("활용 적합도는 임베딩 점수 × 0.3 + LLM 점수 × 0.7로 계산된다")
  void calculateUsageFitScoreWithWeightedFormula() {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createCompletedJd(owner);
    setId(jd, 10L);

    // pieceId=100, 임베딩=60, LLM=80 → 활용적합도 = round(60*0.3 + 80*0.7) = round(18+56) = 74
    Piece piece = Piece.builder().type(PieceType.ACTIVITY).user(owner).build();
    setId(piece, 100L);
    Experience exp = createExperienceWithPiece(piece);
    setId(exp, 30L);

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(experienceRepository.findAllByUserIdForAnalysis(userId)).thenReturn(List.of(exp));
    when(jdMatchRepository.findSimilaritiesByJdAndUser(10L, userId))
        .thenReturn(List.of(new PieceSimilarity(100L, 60)));
    when(llmMatchScoreService.scoreAll(eq(jd), any()))
        .thenReturn(new LlmMatchResult(Map.of(100L, 80), 70));
    when(tagRepository.findByExperienceIdIn(List.of(30L))).thenReturn(List.of());

    JdMatchAnalysisResponse response = jdMatchService.analyze(10L, userId);

    assertThat(response.matchResult().experiences()).hasSize(1);
    assertThat(response.matchResult().experiences().get(0).usageFitScore()).isEqualTo(74);
  }

  @Test
  @DisplayName("임베딩 점수가 없는 경험은 임베딩 점수 0으로 처리한다")
  void useZeroEmbeddingScoreWhenNotFound() {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createCompletedJd(owner);
    setId(jd, 10L);

    // pieceId=100, 임베딩 점수 없음(0), LLM=50 → 활용적합도 = round(0*0.3 + 50*0.7) = 35
    Piece piece = Piece.builder().type(PieceType.ACTIVITY).user(owner).build();
    setId(piece, 100L);
    Experience exp = createExperienceWithPiece(piece);
    setId(exp, 30L);

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(experienceRepository.findAllByUserIdForAnalysis(userId)).thenReturn(List.of(exp));
    when(jdMatchRepository.findSimilaritiesByJdAndUser(10L, userId)).thenReturn(List.of());
    when(llmMatchScoreService.scoreAll(eq(jd), any()))
        .thenReturn(new LlmMatchResult(Map.of(100L, 50), 40));
    when(tagRepository.findByExperienceIdIn(List.of(30L))).thenReturn(List.of());

    JdMatchAnalysisResponse response = jdMatchService.analyze(10L, userId);

    assertThat(response.matchResult().experiences().get(0).usageFitScore()).isEqualTo(35);
  }

  @Test
  @DisplayName("경험 카드 목록은 활용 적합도 내림차순으로 정렬된다")
  void sortExperienceCardsByUsageFitScoreDesc() {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createCompletedJd(owner);
    setId(jd, 10L);

    // pieceId=101 → 임베딩=80, LLM=90 → 활용적합도 = round(80*0.3+90*0.7) = round(24+63) = 87
    // pieceId=102 → 임베딩=40, LLM=30 → 활용적합도 = round(40*0.3+30*0.7) = round(12+21) = 33
    Piece piece1 = Piece.builder().type(PieceType.ACTIVITY).user(owner).build();
    setId(piece1, 101L);
    Piece piece2 = Piece.builder().type(PieceType.CAREER).user(owner).build();
    setId(piece2, 102L);
    Experience exp1 = createExperienceWithPiece(piece1);
    setId(exp1, 31L);
    Experience exp2 = createExperienceWithPiece(piece2);
    setId(exp2, 32L);

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(experienceRepository.findAllByUserIdForAnalysis(userId))
        .thenReturn(List.of(exp2, exp1)); // 의도적으로 낮은 점수 먼저
    when(jdMatchRepository.findSimilaritiesByJdAndUser(10L, userId))
        .thenReturn(List.of(new PieceSimilarity(101L, 80), new PieceSimilarity(102L, 40)));
    when(llmMatchScoreService.scoreAll(eq(jd), any()))
        .thenReturn(new LlmMatchResult(Map.of(101L, 90, 102L, 30), 60));
    when(tagRepository.findByExperienceIdIn(any())).thenReturn(List.of());

    JdMatchAnalysisResponse response = jdMatchService.analyze(10L, userId);

    List<JdMatchAnalysisResponse.ExperienceMatchCard> cards = response.matchResult().experiences();
    assertThat(cards).hasSize(2);
    assertThat(cards.get(0).usageFitScore()).isGreaterThan(cards.get(1).usageFitScore());
    assertThat(cards.get(0).experienceId()).isEqualTo(31L);
    assertThat(cards.get(1).experienceId()).isEqualTo(32L);
  }

  @Test
  @DisplayName("hardSkill과 softSkill은 쉼표로 분리되어 JdInfo에 리스트로 담긴다")
  void parseSkillTagsIntoListInJdInfo() {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createCompletedJd(owner);
    setId(jd, 10L);

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(experienceRepository.findAllByUserIdForAnalysis(userId)).thenReturn(List.of());

    JdMatchAnalysisResponse response = jdMatchService.analyze(10L, userId);

    assertThat(response.jdInfo().hardSkills()).containsExactly("Java", "Spring");
    assertThat(response.jdInfo().softSkills()).containsExactly("협업", "문제 해결");
  }

  private User createUser(Long id) {
    User user =
        User.basicLoginBuilder()
            .name("테스트 유저")
            .email("test" + id + "@example.com")
            .password("encoded-password")
            .build();
    setId(user, id);
    return user;
  }

  private Jd createJd(User owner, String hardSkill, String softSkill) {
    return Jd.builder()
        .user(owner)
        .postingTitle("백엔드 개발자")
        .companyName("끼움")
        .recruitmentField("백엔드")
        .hardSkill(hardSkill)
        .softSkill(softSkill)
        .rawText("공고 본문")
        .build();
  }

  /** COMPLETED 상태의 JD를 생성한다 (hardSkill, softSkill 기본값 포함) */
  private Jd createCompletedJd(User owner) {
    Jd jd = createJd(owner, "Java, Spring", "협업, 문제 해결");
    jd.updateAnalysisStatus(AnalysisStatus.COMPLETED);
    jd.updateAnalysis("주요 업무", "필수 자격", "우대 사항", "Java, Spring", "협업, 문제 해결");
    return jd;
  }

  private Experience createExperienceWithPiece(Piece piece) {
    return Experience.builder()
        .piece(piece)
        .title("정산 배치 안정화")
        .oneLineIntro("장애율을 낮춘 프로젝트")
        .situation("배치가 자주 실패했습니다.")
        .task("안정화가 필요했습니다.")
        .act("재시도 로직을 개선했습니다.")
        .result("실패율을 낮췄습니다.")
        .taken("모니터링의 중요성을 배웠습니다.")
        .build();
  }

  private void setId(Object target, Long id) {
    try {
      Field field = target.getClass().getDeclaredField("id");
      field.setAccessible(true);
      field.set(target, id);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Failed to set test id", e);
    }
  }
}
