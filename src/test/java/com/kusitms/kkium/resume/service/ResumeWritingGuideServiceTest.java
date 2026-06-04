package com.kusitms.kkium.resume.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_SELECTION_LIMIT;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.FORBIDDEN;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.JD_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.QUESTION_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.domain.Piece;
import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.experience.repository.ExperienceRepository;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.jd.repository.JdQuestionRepository;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.jd.utils.llm.LlmMatchScoreService;
import com.kusitms.kkium.jd.utils.llm.result.LlmWritingGuideResult;
import com.kusitms.kkium.resume.dto.response.ResumeWritingGuideResponse;
import com.kusitms.kkium.user.domain.User;

@ExtendWith(MockitoExtension.class)
class ResumeWritingGuideServiceTest {

  @Mock private JdRepository jdRepository;
  @Mock private JdQuestionRepository jdQuestionRepository;
  @Mock private ExperienceRepository experienceRepository;
  @Mock private LlmMatchScoreService llmMatchScoreService;

  private ResumeWritingGuideService resumeWritingGuideService;

  @BeforeEach
  void setUp() {
    resumeWritingGuideService =
        new ResumeWritingGuideService(
            jdRepository, jdQuestionRepository, experienceRepository, llmMatchScoreService);
  }

  @Test
  @DisplayName("experienceIds가 null이면 EXPERIENCE_SELECTION_LIMIT 예외를 던진다")
  void throwWhenExperienceIdsIsNull() {
    assertThatThrownBy(() -> resumeWritingGuideService.generateGuide(1L, 1L, null, 1L))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(EXPERIENCE_SELECTION_LIMIT);

    verify(jdRepository, never()).findById(1L);
  }

  @Test
  @DisplayName("experienceIds가 빈 리스트이면 EXPERIENCE_SELECTION_LIMIT 예외를 던진다")
  void throwWhenExperienceIdsIsEmpty() {
    assertThatThrownBy(() -> resumeWritingGuideService.generateGuide(1L, 1L, List.of(), 1L))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(EXPERIENCE_SELECTION_LIMIT);

    verify(jdRepository, never()).findById(1L);
  }

  @Test
  @DisplayName("experienceIds가 4개 이상이면 EXPERIENCE_SELECTION_LIMIT 예외를 던진다")
  void throwWhenExperienceIdsExceedLimit() {
    assertThatThrownBy(
            () -> resumeWritingGuideService.generateGuide(1L, 1L, List.of(1L, 2L, 3L, 4L), 1L))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(EXPERIENCE_SELECTION_LIMIT);

    verify(jdRepository, never()).findById(1L);
  }

  @Test
  @DisplayName("JD 소유자가 아닌 유저가 요청하면 FORBIDDEN 예외를 던진다")
  void throwWhenUserIsNotJdOwner() {
    Long jdOwnerId = 1L;
    Long requestUserId = 2L;
    Jd jd = createJd(createUser(jdOwnerId));
    setId(jd, 10L);

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));

    assertThatThrownBy(
            () -> resumeWritingGuideService.generateGuide(10L, 1L, List.of(1L), requestUserId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(FORBIDDEN);

    verify(jdQuestionRepository, never()).findById(1L);
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
    JdQuestion question = createQuestion(jd);
    setId(question, 20L);
    Experience otherExperience = createExperience(other);
    setId(otherExperience, 30L);

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(jdQuestionRepository.findById(20L)).thenReturn(Optional.of(question));
    when(experienceRepository.findAllByIdIn(List.of(30L))).thenReturn(List.of(otherExperience));

    assertThatThrownBy(
            () -> resumeWritingGuideService.generateGuide(10L, 20L, List.of(30L), userId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(FORBIDDEN);

    verify(llmMatchScoreService, never())
        .generateWritingGuide(jd, question, List.of(otherExperience));
  }

  @Test
  @DisplayName("존재하지 않는 JD이면 JD_NOT_FOUND 예외를 던진다")
  void throwWhenJdNotFound() {
    when(jdRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> resumeWritingGuideService.generateGuide(999L, 1L, List.of(1L), 1L))
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
            () -> resumeWritingGuideService.generateGuide(10L, 999L, List.of(1L), userId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(QUESTION_NOT_FOUND);
  }

  @Test
  @DisplayName("조회된 경험 수가 요청한 experienceIds 수와 다르면 EXPERIENCE_NOT_FOUND 예외를 던진다")
  void throwWhenSomeExperiencesNotFound() {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createJd(owner);
    setId(jd, 10L);
    JdQuestion question = createQuestion(jd);
    setId(question, 20L);
    Experience experience = createExperience(owner);
    setId(experience, 30L);

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(jdQuestionRepository.findById(20L)).thenReturn(Optional.of(question));
    // 2개 요청했는데 1개만 반환
    when(experienceRepository.findAllByIdIn(List.of(30L, 99L))).thenReturn(List.of(experience));

    assertThatThrownBy(
            () -> resumeWritingGuideService.generateGuide(10L, 20L, List.of(30L, 99L), userId))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(EXPERIENCE_NOT_FOUND);
  }

  @Test
  @DisplayName("정상 요청이면 LLM 결과를 그대로 반환한다")
  void generateGuideSuccessfully() {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createJd(owner);
    setId(jd, 10L);
    JdQuestion question = createQuestion(jd);
    setId(question, 20L);
    Experience experience = createExperience(owner);
    setId(experience, 30L);
    LlmWritingGuideResult llmResult =
        new LlmWritingGuideResult(
            List.of("Java", "Spring"),
            "백엔드 개발 경험이 JD의 핵심 역량과 잘 일치합니다.",
            "STAR 구조를 활용하여 문제 해결 과정을 구체적으로 서술하세요.");

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(jdQuestionRepository.findById(20L)).thenReturn(Optional.of(question));
    when(experienceRepository.findAllByIdIn(List.of(30L))).thenReturn(List.of(experience));
    when(llmMatchScoreService.generateWritingGuide(jd, question, List.of(experience)))
        .thenReturn(llmResult);

    ResumeWritingGuideResponse response =
        resumeWritingGuideService.generateGuide(10L, 20L, List.of(30L), userId);

    assertThat(response.coreKeywords()).containsExactly("Java", "Spring");
    assertThat(response.connectionToJd()).isEqualTo("백엔드 개발 경험이 JD의 핵심 역량과 잘 일치합니다.");
    assertThat(response.writingGuide()).isEqualTo("STAR 구조를 활용하여 문제 해결 과정을 구체적으로 서술하세요.");
    verify(llmMatchScoreService).generateWritingGuide(jd, question, List.of(experience));
  }

  @Test
  @DisplayName("경험을 최대 3개까지 선택하면 모두 LLM에 전달한다")
  void generateGuideWithMaxExperiences() {
    Long userId = 1L;
    User owner = createUser(userId);
    Jd jd = createJd(owner);
    setId(jd, 10L);
    JdQuestion question = createQuestion(jd);
    setId(question, 20L);
    Experience exp1 = createExperience(owner);
    setId(exp1, 31L);
    Experience exp2 = createExperience(owner);
    setId(exp2, 32L);
    Experience exp3 = createExperience(owner);
    setId(exp3, 33L);
    LlmWritingGuideResult llmResult =
        new LlmWritingGuideResult(List.of("협업"), "다양한 경험이 있습니다.", "경험을 통합하여 서술하세요.");

    when(jdRepository.findById(10L)).thenReturn(Optional.of(jd));
    when(jdQuestionRepository.findById(20L)).thenReturn(Optional.of(question));
    when(experienceRepository.findAllByIdIn(List.of(31L, 32L, 33L)))
        .thenReturn(List.of(exp1, exp2, exp3));
    when(llmMatchScoreService.generateWritingGuide(jd, question, List.of(exp1, exp2, exp3)))
        .thenReturn(llmResult);

    ResumeWritingGuideResponse response =
        resumeWritingGuideService.generateGuide(10L, 20L, List.of(31L, 32L, 33L), userId);

    assertThat(response.coreKeywords()).containsExactly("협업");
    verify(llmMatchScoreService).generateWritingGuide(jd, question, List.of(exp1, exp2, exp3));
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
