package com.kusitms.kkium.resume.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.JD_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.QUESTION_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
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
import com.kusitms.kkium.experience.service.ExperiencePeriodResolver;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.jd.repository.JdMatchRepository;
import com.kusitms.kkium.jd.repository.JdMatchRepository.PieceSimilarity;
import com.kusitms.kkium.jd.repository.JdQuestionRepository;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.jd.utils.llm.LlmMatchScoreService;
import com.kusitms.kkium.jd.utils.llm.result.LlmQuestionMatchResult;
import com.kusitms.kkium.resume.dto.response.ResumeQuestionExperienceResponse;
import com.kusitms.kkium.resume.dto.response.ResumeQuestionExperienceResponse.ExperienceMatchItem;
import com.kusitms.kkium.user.domain.User;

@ExtendWith(MockitoExtension.class)
class ResumeQuestionExperienceServiceTest {

  @Mock private JdRepository jdRepository;
  @Mock private JdQuestionRepository jdQuestionRepository;
  @Mock private ExperienceRepository experienceRepository;
  @Mock private JdMatchRepository jdMatchRepository;
  @Mock private LlmMatchScoreService llmMatchScoreService;
  @Mock private ExperiencePeriodResolver experiencePeriodResolver;

  private ResumeQuestionExperienceService resumeQuestionExperienceService;

  @BeforeEach
  void setUp() {
    resumeQuestionExperienceService =
        new ResumeQuestionExperienceService(
            jdRepository,
            jdQuestionRepository,
            experienceRepository,
            jdMatchRepository,
            llmMatchScoreService,
            experiencePeriodResolver);
  }

  @Test
  @DisplayName("존재하지 않는 JD이면 JD_NOT_FOUND 예외를 던진다")
  void throwWhenJdNotFound() {
    when(jdRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> resumeQuestionExperienceService.getExperiencesWithFitScore(999L, 1L, 1L))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(JD_NOT_FOUND);
  }

  @Test
  @DisplayName("존재하지 않는 문항이면 QUESTION_NOT_FOUND 예외를 던진다")
  void throwWhenQuestionNotFound() {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createJd(owner);
    setId(jd, 10L);

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(jdQuestionRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> resumeQuestionExperienceService.getExperiencesWithFitScore(10L, 999L, userId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(QUESTION_NOT_FOUND);
  }

  @Test
  @DisplayName("유저 경험이 없으면 LLM 호출 없이 빈 리스트를 반환한다")
  void returnEmptyListWithoutLlmCallWhenNoExperiences() {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createJd(owner);
    setId(jd, 10L);
    JdQuestion question = createQuestion(jd);
    setId(question, 20L);

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(jdQuestionRepository.findById(20L)).thenReturn(Optional.of(question));
    when(experienceRepository.findAllByUserIdNoPage(userId)).thenReturn(List.of());

    ResumeQuestionExperienceResponse response =
        resumeQuestionExperienceService.getExperiencesWithFitScore(10L, 20L, userId);

    assertThat(response.experiences()).isEmpty();
    verify(llmMatchScoreService, never()).scoreAllByQuestion(any(), any(), any());
  }

  @Test
  @DisplayName("활용 적합도는 임베딩 점수 × 0.3 + LLM 점수 × 0.7로 계산된다")
  void calculateUsageFitScoreWithWeightedFormula() {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createJd(owner);
    setId(jd, 10L);
    JdQuestion question = createQuestion(jd);
    setId(question, 20L);

    // pieceId=100, 임베딩=60, LLM=80 → 활용적합도 = round(60*0.3 + 80*0.7) = round(18+56) = 74
    Piece piece = Piece.builder().type(PieceType.ACTIVITY).user(owner).build();
    setId(piece, 100L);
    Experience exp = createExperienceWithPiece(piece);
    setId(exp, 30L);

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(jdQuestionRepository.findById(20L)).thenReturn(Optional.of(question));
    when(experienceRepository.findAllByUserIdNoPage(userId)).thenReturn(List.of(exp));
    when(jdMatchRepository.findSimilaritiesByJdAndUser(10L, userId))
        .thenReturn(List.of(new PieceSimilarity(100L, 60)));
    when(llmMatchScoreService.scoreAllByQuestion(eq(jd), eq(question), any()))
        .thenReturn(new LlmQuestionMatchResult(Map.of(100L, 80)));
    when(experiencePeriodResolver.resolvePeriodBulk(any())).thenReturn(Map.of());

    ResumeQuestionExperienceResponse response =
        resumeQuestionExperienceService.getExperiencesWithFitScore(10L, 20L, userId);

    assertThat(response.experiences()).hasSize(1);
    assertThat(response.experiences().get(0).usageFitScore()).isEqualTo(74);
  }

  @Test
  @DisplayName("임베딩 점수가 없는 경험은 임베딩 점수 0으로 처리한다")
  void useZeroEmbeddingScoreWhenNotFound() {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createJd(owner);
    setId(jd, 10L);
    JdQuestion question = createQuestion(jd);
    setId(question, 20L);

    // pieceId=100, 임베딩 없음(0), LLM=50 → 활용적합도 = round(0*0.3 + 50*0.7) = 35
    Piece piece = Piece.builder().type(PieceType.ACTIVITY).user(owner).build();
    setId(piece, 100L);
    Experience exp = createExperienceWithPiece(piece);
    setId(exp, 30L);

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(jdQuestionRepository.findById(20L)).thenReturn(Optional.of(question));
    when(experienceRepository.findAllByUserIdNoPage(userId)).thenReturn(List.of(exp));
    when(jdMatchRepository.findSimilaritiesByJdAndUser(10L, userId)).thenReturn(List.of());
    when(llmMatchScoreService.scoreAllByQuestion(eq(jd), eq(question), any()))
        .thenReturn(new LlmQuestionMatchResult(Map.of(100L, 50)));
    when(experiencePeriodResolver.resolvePeriodBulk(any())).thenReturn(Map.of());

    ResumeQuestionExperienceResponse response =
        resumeQuestionExperienceService.getExperiencesWithFitScore(10L, 20L, userId);

    assertThat(response.experiences().get(0).usageFitScore()).isEqualTo(35);
  }

  @Test
  @DisplayName("경험 목록은 활용 적합도 내림차순으로 정렬된다")
  void sortExperiencesByUsageFitScoreDesc() {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createJd(owner);
    setId(jd, 10L);
    JdQuestion question = createQuestion(jd);
    setId(question, 20L);

    // pieceId=101 → 임베딩=80, LLM=90 → 활용적합도 = round(80*0.3+90*0.7) = 87
    // pieceId=102 → 임베딩=40, LLM=30 → 활용적합도 = round(40*0.3+30*0.7) = 33
    Piece piece1 = Piece.builder().type(PieceType.ACTIVITY).user(owner).build();
    setId(piece1, 101L);
    Piece piece2 = Piece.builder().type(PieceType.CAREER).user(owner).build();
    setId(piece2, 102L);
    Experience exp1 = createExperienceWithPiece(piece1);
    setId(exp1, 31L);
    Experience exp2 = createExperienceWithPiece(piece2);
    setId(exp2, 32L);

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(jdQuestionRepository.findById(20L)).thenReturn(Optional.of(question));
    when(experienceRepository.findAllByUserIdNoPage(userId))
        .thenReturn(List.of(exp2, exp1)); // 낮은 점수 먼저 투입
    when(jdMatchRepository.findSimilaritiesByJdAndUser(10L, userId))
        .thenReturn(List.of(new PieceSimilarity(101L, 80), new PieceSimilarity(102L, 40)));
    when(llmMatchScoreService.scoreAllByQuestion(eq(jd), eq(question), any()))
        .thenReturn(new LlmQuestionMatchResult(Map.of(101L, 90, 102L, 30)));
    when(experiencePeriodResolver.resolvePeriodBulk(any())).thenReturn(Map.of());

    ResumeQuestionExperienceResponse response =
        resumeQuestionExperienceService.getExperiencesWithFitScore(10L, 20L, userId);

    List<ExperienceMatchItem> items = response.experiences();
    assertThat(items).hasSize(2);
    assertThat(items.get(0).usageFitScore()).isGreaterThan(items.get(1).usageFitScore());
    assertThat(items.get(0).experienceId()).isEqualTo(31L);
    assertThat(items.get(1).experienceId()).isEqualTo(32L);
  }

  @Test
  @DisplayName("기간 정보가 있는 경험은 시작일과 종료일이 응답에 포함된다")
  void includePeriodInResponseWhenResolved() {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createJd(owner);
    setId(jd, 10L);
    JdQuestion question = createQuestion(jd);
    setId(question, 20L);
    Piece piece = Piece.builder().type(PieceType.ACTIVITY).user(owner).build();
    setId(piece, 100L);
    Experience exp = createExperienceWithPiece(piece);
    setId(exp, 30L);
    LocalDate start = LocalDate.of(2024, 1, 1);
    LocalDate end = LocalDate.of(2024, 6, 30);

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(jdQuestionRepository.findById(20L)).thenReturn(Optional.of(question));
    when(experienceRepository.findAllByUserIdNoPage(userId)).thenReturn(List.of(exp));
    when(jdMatchRepository.findSimilaritiesByJdAndUser(10L, userId)).thenReturn(List.of());
    when(llmMatchScoreService.scoreAllByQuestion(eq(jd), eq(question), any()))
        .thenReturn(new LlmQuestionMatchResult(Map.of(100L, 60)));
    when(experiencePeriodResolver.resolvePeriodBulk(any()))
        .thenReturn(Map.of(30L, new LocalDate[] {start, end}));

    ResumeQuestionExperienceResponse response =
        resumeQuestionExperienceService.getExperiencesWithFitScore(10L, 20L, userId);

    ExperienceMatchItem item = response.experiences().get(0);
    assertThat(item.startDate()).isEqualTo(start);
    assertThat(item.endDate()).isEqualTo(end);
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

  private Jd createJd(User owner) {
    return Jd.builder()
        .user(owner)
        .postingTitle("백엔드 개발자")
        .companyName("끼움")
        .recruitmentField("백엔드")
        .rawText("공고 본문")
        .build();
  }

  private JdQuestion createQuestion(Jd jd) {
    return JdQuestion.builder().jd(jd).orderNum(1).content("지원 동기를 작성해주세요.").build();
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
