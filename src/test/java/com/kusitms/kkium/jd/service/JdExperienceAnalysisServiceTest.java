package com.kusitms.kkium.jd.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.FORBIDDEN;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.JD_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.domain.Piece;
import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.experience.repository.ExperienceRepository;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.dto.response.JdExperienceAnalysisResponse;
import com.kusitms.kkium.jd.dto.response.JdExperienceAnalysisResponse.ExperienceAnalysis;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.jd.utils.llm.LlmMatchScoreService;
import com.kusitms.kkium.jd.utils.llm.result.LlmExperienceDetailResult;
import com.kusitms.kkium.user.domain.User;

@ExtendWith(MockitoExtension.class)
class JdExperienceAnalysisServiceTest {

  @Mock private JdRepository jdRepository;
  @Mock private ExperienceRepository experienceRepository;
  @Mock private LlmMatchScoreService llmMatchScoreService;
  @Mock private StringRedisTemplate redisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;

  private JdExperienceAnalysisService jdExperienceAnalysisService;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    jdExperienceAnalysisService =
        new JdExperienceAnalysisService(
            jdRepository, experienceRepository, llmMatchScoreService, redisTemplate, objectMapper);
  }

  // ── NOT FOUND ──────────────────────────────────────────────

  @Test
  @DisplayName("존재하지 않는 JD이면 JD_NOT_FOUND 예외를 던진다")
  void throwWhenJdNotFound() {
    when(jdRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> jdExperienceAnalysisService.analyze(999L, 1L, 1L))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(JD_NOT_FOUND);
  }

  @Test
  @DisplayName("존재하지 않는 경험이면 EXPERIENCE_NOT_FOUND 예외를 던진다")
  void throwWhenExperienceNotFound() {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createJd(owner);
    setId(jd, 10L);

    // 실행 순서: 1.JD조회 → 2.JD소유자검증 → 3.경험조회(여기서 예외) → 4... → 5.Redis
    // Redis stub 불필요
    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(experienceRepository.findByIdWithPiece(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> jdExperienceAnalysisService.analyze(10L, 999L, userId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(EXPERIENCE_NOT_FOUND);
  }

  @Test
  @DisplayName("JD 소유자가 아닌 유저가 요청하면 FORBIDDEN 예외를 던진다")
  void throwWhenUserIsNotJdOwner() {
    Long jdOwnerId = 1L;
    Long requestUserId = 2L;
    User owner = createUser(jdOwnerId);
    Jd jd = createJd(owner);
    setId(jd, 10L);

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));

    assertThatThrownBy(() -> jdExperienceAnalysisService.analyze(10L, 1L, requestUserId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(FORBIDDEN);
  }

  @Test
  @DisplayName("경험 소유자가 다른 유저이면 FORBIDDEN 예외를 던진다")
  void throwWhenUserIsNotExperienceOwner() {
    Long userId = 1L;
    Long otherUserId = 2L;
    User owner = createUser(userId);
    User other = createUser(otherUserId);
    Jd jd = createJd(owner);
    setId(jd, 10L);
    Experience otherExperience = createExperience(other);
    setId(otherExperience, 30L);

    // 실행 순서: 1.JD조회 → 2.JD소유자검증 → 3.경험조회 → 4.경험소유자검증(여기서 예외) → 5.Redis
    // Redis stub 불필요
    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(experienceRepository.findByIdWithPiece(30L)).thenReturn(Optional.of(otherExperience));

    assertThatThrownBy(() -> jdExperienceAnalysisService.analyze(10L, 30L, userId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(FORBIDDEN);
  }

  @Test
  @DisplayName("Redis 캐시 히트 시 LLM을 호출하지 않고 캐시 결과를 반환한다")
  void returnCachedResultWithoutLlmCall() throws Exception {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createJd(owner);
    setId(jd, 10L);
    Experience experience = createExperience(owner);
    setId(experience, 30L);

    JdExperienceAnalysisResponse cached =
        new JdExperienceAnalysisResponse(
            30L, new ExperienceAnalysis("강점입니다.", "약점입니다.", "활용 가이드입니다.", List.of()));
    String cachedJson = objectMapper.writeValueAsString(cached);

    // 실행 순서: 1.JD조회 → 2.JD소유자검증 → 3.경험조회 → 4.경험소유자검증 → 5.Redis(캐시 히트 → 즉시 반환)
    // 3번 경험 조회가 Redis보다 먼저 실행되므로 stub 필요
    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(experienceRepository.findByIdWithPiece(30L)).thenReturn(Optional.of(experience));
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("jd-analysis:30:10")).thenReturn(cachedJson);

    JdExperienceAnalysisResponse response = jdExperienceAnalysisService.analyze(10L, 30L, userId);

    assertThat(response.experienceId()).isEqualTo(30L);
    assertThat(response.analysis().strengths()).isEqualTo("강점입니다.");
    verify(llmMatchScoreService, never()).analyzeExperienceDetail(any(), any());
  }

  @Test
  @DisplayName("Redis 캐시 미스 시 LLM을 호출하고 결과를 캐시에 저장한다")
  void callLlmAndSaveCacheOnCacheMiss() throws Exception {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createJd(owner);
    setId(jd, 10L);
    Experience experience = createExperience(owner);
    setId(experience, 30L);
    LlmExperienceDetailResult llmResult =
        new LlmExperienceDetailResult("강점입니다.", "약점입니다.", "활용 가이드입니다.", List.of());

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    when(valueOperations.get("jd-analysis:30:10")).thenReturn(null);
    when(experienceRepository.findByIdWithPiece(30L)).thenReturn(Optional.of(experience));
    when(llmMatchScoreService.analyzeExperienceDetail(jd, experience)).thenReturn(llmResult);

    JdExperienceAnalysisResponse response = jdExperienceAnalysisService.analyze(10L, 30L, userId);

    assertThat(response.experienceId()).isEqualTo(30L);
    assertThat(response.analysis().strengths()).isEqualTo("강점입니다.");
    assertThat(response.analysis().weaknesses()).isEqualTo("약점입니다.");
    assertThat(response.analysis().usageGuide()).isEqualTo("활용 가이드입니다.");
    verify(llmMatchScoreService).analyzeExperienceDetail(jd, experience);
    verify(valueOperations).set(eq("jd-analysis:30:10"), anyString(), any());
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
        .hardSkill("Java, Spring")
        .softSkill("협업")
        .rawText("공고 본문")
        .build();
  }

  private Experience createExperience(User owner) {
    Piece piece = Piece.builder().type(PieceType.ACTIVITY).user(owner).build();
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
