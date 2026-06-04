package com.kusitms.kkium.experience.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_COMPANY_TOO_LONG;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_EDUCATION_NAME_TOO_LONG;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_ORGANIZATION_NAME_TOO_LONG;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_ROLE_TOO_LONG;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.FORBIDDEN;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.INVALID_INPUT_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.kusitms.kkium.experience.domain.Activity;
import com.kusitms.kkium.experience.domain.Career;
import com.kusitms.kkium.experience.domain.Education;
import com.kusitms.kkium.experience.domain.Etc;
import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.domain.Piece;
import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.experience.domain.type.TagCategory;
import com.kusitms.kkium.experience.dto.request.ExperienceUpdateRequest;
import com.kusitms.kkium.experience.dto.request.ExperienceUpdateRequest.Detail;
import com.kusitms.kkium.experience.dto.request.TagCreateRequest;
import com.kusitms.kkium.experience.repository.ActivityRepository;
import com.kusitms.kkium.experience.repository.CareerRepository;
import com.kusitms.kkium.experience.repository.EducationRepository;
import com.kusitms.kkium.experience.repository.EtcRepository;
import com.kusitms.kkium.experience.repository.ExperienceOrderRepository;
import com.kusitms.kkium.experience.repository.ExperienceRepository;
import com.kusitms.kkium.experience.repository.PieceRepository;
import com.kusitms.kkium.experience.repository.TagRepository;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.home.service.JobTypeUpdateService;
import com.kusitms.kkium.jd.service.JdExperienceAnalysisService;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ExperienceServiceUpdateTest {

  @Mock private UserRepository userRepository;
  @Mock private PieceRepository pieceRepository;
  @Mock private ExperienceRepository experienceRepository;
  @Mock private ExperienceOrderRepository experienceOrderRepository;
  @Mock private ActivityRepository activityRepository;
  @Mock private CareerRepository careerRepository;
  @Mock private EducationRepository educationRepository;
  @Mock private EtcRepository etcRepository;
  @Mock private TagRepository tagRepository;
  @Mock private ExperienceEmbeddingService experienceEmbeddingService;
  @Mock private JobTypeUpdateService jobTypeUpdateService;
  @Mock private JdExperienceAnalysisService jdExperienceAnalysisService;
  @Mock private ExperiencePeriodResolver experiencePeriodResolver;

  private ExperienceService experienceService;

  @BeforeEach
  void setUp() {
    TransactionSynchronizationManager.initSynchronization();
    experienceService =
        new ExperienceService(
            userRepository,
            pieceRepository,
            experienceRepository,
            experienceOrderRepository,
            activityRepository,
            careerRepository,
            educationRepository,
            etcRepository,
            tagRepository,
            experienceEmbeddingService,
            jobTypeUpdateService,
            jdExperienceAnalysisService,
            experiencePeriodResolver);
  }

  @AfterEach
  void tearDown() {
    TransactionSynchronizationManager.clearSynchronization();
  }

  @Test
  @DisplayName("존재하지 않는 경험이면 EXPERIENCE_NOT_FOUND 예외를 던진다")
  void throwWhenExperienceNotFound() {
    when(experienceRepository.findByIdWithPiece(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> experienceService.update(1L, 999L, activityRequest("제목")))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(EXPERIENCE_NOT_FOUND);
  }

  @Test
  @DisplayName("경험 소유자가 아닌 유저가 수정 요청하면 FORBIDDEN 예외를 던진다")
  void throwWhenUserIsNotExperienceOwner() {
    Long ownerId = 1L;
    Long requestUserId = 2L;
    User owner = createUser(ownerId);
    Experience experience = createExperience(owner, PieceType.ACTIVITY);
    setId(experience, 10L);

    when(experienceRepository.findByIdWithPiece(10L)).thenReturn(Optional.of(experience));

    assertThatThrownBy(() -> experienceService.update(requestUserId, 10L, activityRequest("제목")))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(FORBIDDEN);
  }

  @Test
  @DisplayName("ACTIVITY 수정 시 공통 필드와 activity detail이 모두 업데이트된다")
  void updateActivityExperience() {
    Long userId = 1L;
    User owner = createUser(userId);
    Experience experience = createExperience(owner, PieceType.ACTIVITY);
    setId(experience, 10L);
    Activity activity = createActivity(experience);

    when(experienceRepository.findByIdWithPiece(10L)).thenReturn(Optional.of(experience));
    when(tagRepository.findByExperienceId(10L)).thenReturn(List.of());
    when(activityRepository.findByExperienceId(10L)).thenReturn(Optional.of(activity));

    ExperienceUpdateRequest request = activityRequest("수정된 제목");
    experienceService.update(userId, 10L, request);

    assertThat(experience.getTitle()).isEqualTo("수정된 제목");
    assertThat(experience.getOneLineIntro()).isEqualTo("한 줄 소개");
    assertThat(activity.getName()).isEqualTo("끼움 동아리");
    assertThat(activity.getRole()).isEqualTo("백엔드");
    verify(jdExperienceAnalysisService).evictCache(10L);
  }

  @Test
  @DisplayName("ACTIVITY 수정 시 필수 필드(name)가 null이면 INVALID_INPUT_VALUE 예외를 던진다")
  void throwWhenActivityNameIsNull() {
    Long userId = 1L;
    User owner = createUser(userId);
    Experience experience = createExperience(owner, PieceType.ACTIVITY);
    setId(experience, 10L);

    when(experienceRepository.findByIdWithPiece(10L)).thenReturn(Optional.of(experience));
    when(tagRepository.findByExperienceId(10L)).thenReturn(List.of());

    // name=null인 ACTIVITY 요청
    ExperienceUpdateRequest request =
        new ExperienceUpdateRequest(
            "제목",
            "한 줄 소개",
            List.of(),
            "S",
            "T",
            "A",
            "R",
            "L",
            new Detail(
                null,
                5,
                "백엔드",
                80,
                null,
                null,
                null,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 6, 30)));

    assertThatThrownBy(() -> experienceService.update(userId, 10L, request))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(INVALID_INPUT_VALUE);
  }

  @Test
  @DisplayName("ACTIVITY role이 50자 초과이면 EXPERIENCE_ROLE_TOO_LONG 예외를 던진다")
  void throwWhenActivityRoleTooLong() {
    Long userId = 1L;
    User owner = createUser(userId);
    Experience experience = createExperience(owner, PieceType.ACTIVITY);
    setId(experience, 10L);

    when(experienceRepository.findByIdWithPiece(10L)).thenReturn(Optional.of(experience));
    when(tagRepository.findByExperienceId(10L)).thenReturn(List.of());

    String tooLongRole = "a".repeat(51);
    ExperienceUpdateRequest request =
        new ExperienceUpdateRequest(
            "제목",
            "한 줄 소개",
            List.of(),
            "S",
            "T",
            "A",
            "R",
            "L",
            new Detail(
                "끼움",
                5,
                tooLongRole,
                80,
                null,
                null,
                null,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 6, 30)));

    assertThatThrownBy(() -> experienceService.update(userId, 10L, request))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(EXPERIENCE_ROLE_TOO_LONG);
  }

  @Test
  @DisplayName("CAREER 수정 시 공통 필드와 career detail이 모두 업데이트된다")
  void updateCareerExperience() {
    Long userId = 1L;
    User owner = createUser(userId);
    Experience experience = createExperience(owner, PieceType.CAREER);
    setId(experience, 10L);
    Career career = createCareer(experience);

    when(experienceRepository.findByIdWithPiece(10L)).thenReturn(Optional.of(experience));
    when(tagRepository.findByExperienceId(10L)).thenReturn(List.of());
    when(careerRepository.findByExperienceId(10L)).thenReturn(Optional.of(career));

    ExperienceUpdateRequest request = careerRequest("수정된 제목");
    experienceService.update(userId, 10L, request);

    assertThat(experience.getTitle()).isEqualTo("수정된 제목");
    assertThat(career.getCompany()).isEqualTo("끼움 회사");
    verify(jdExperienceAnalysisService).evictCache(10L);
  }

  @Test
  @DisplayName("CAREER company가 50자 초과이면 EXPERIENCE_COMPANY_TOO_LONG 예외를 던진다")
  void throwWhenCareerCompanyTooLong() {
    Long userId = 1L;
    User owner = createUser(userId);
    Experience experience = createExperience(owner, PieceType.CAREER);
    setId(experience, 10L);

    when(experienceRepository.findByIdWithPiece(10L)).thenReturn(Optional.of(experience));
    when(tagRepository.findByExperienceId(10L)).thenReturn(List.of());

    String tooLongCompany = "a".repeat(51);
    ExperienceUpdateRequest request =
        new ExperienceUpdateRequest(
            "제목",
            "한 줄 소개",
            List.of(),
            "S",
            "T",
            "A",
            "R",
            "L",
            new Detail(
                null,
                null,
                null,
                null,
                tooLongCompany,
                "인턴",
                null,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 6, 30)));

    assertThatThrownBy(() -> experienceService.update(userId, 10L, request))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(EXPERIENCE_COMPANY_TOO_LONG);
  }

  @Test
  @DisplayName("EDUCATION 수정 시 공통 필드와 education detail이 모두 업데이트된다")
  void updateEducationExperience() {
    Long userId = 1L;
    User owner = createUser(userId);
    Experience experience = createExperience(owner, PieceType.EDUCATION);
    setId(experience, 10L);
    Education education = createEducation(experience);

    when(experienceRepository.findByIdWithPiece(10L)).thenReturn(Optional.of(experience));
    when(tagRepository.findByExperienceId(10L)).thenReturn(List.of());
    when(educationRepository.findByExperienceId(10L)).thenReturn(Optional.of(education));

    ExperienceUpdateRequest request = educationRequest("수정된 제목");
    experienceService.update(userId, 10L, request);

    assertThat(experience.getTitle()).isEqualTo("수정된 제목");
    assertThat(education.getOrganizationName()).isEqualTo("끼움 대학교");
    verify(jdExperienceAnalysisService).evictCache(10L);
  }

  @Test
  @DisplayName("EDUCATION organizationName이 50자 초과이면 EXPERIENCE_ORGANIZATION_NAME_TOO_LONG 예외를 던진다")
  void throwWhenEducationOrganizationNameTooLong() {
    Long userId = 1L;
    User owner = createUser(userId);
    Experience experience = createExperience(owner, PieceType.EDUCATION);
    setId(experience, 10L);

    when(experienceRepository.findByIdWithPiece(10L)).thenReturn(Optional.of(experience));
    when(tagRepository.findByExperienceId(10L)).thenReturn(List.of());

    String tooLongOrgName = "a".repeat(51);
    ExperienceUpdateRequest request =
        new ExperienceUpdateRequest(
            "제목",
            "한 줄 소개",
            List.of(),
            "S",
            "T",
            "A",
            "R",
            "L",
            new Detail(
                "스프링 기초",
                null,
                null,
                null,
                null,
                null,
                tooLongOrgName,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 6, 30)));

    assertThatThrownBy(() -> experienceService.update(userId, 10L, request))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(EXPERIENCE_ORGANIZATION_NAME_TOO_LONG);
  }

  @Test
  @DisplayName("EDUCATION name이 80자 초과이면 EXPERIENCE_EDUCATION_NAME_TOO_LONG 예외를 던진다")
  void throwWhenEducationNameTooLong() {
    Long userId = 1L;
    User owner = createUser(userId);
    Experience experience = createExperience(owner, PieceType.EDUCATION);
    setId(experience, 10L);

    when(experienceRepository.findByIdWithPiece(10L)).thenReturn(Optional.of(experience));
    when(tagRepository.findByExperienceId(10L)).thenReturn(List.of());

    String tooLongName = "a".repeat(81);
    ExperienceUpdateRequest request =
        new ExperienceUpdateRequest(
            "제목",
            "한 줄 소개",
            List.of(),
            "S",
            "T",
            "A",
            "R",
            "L",
            new Detail(
                tooLongName,
                null,
                null,
                null,
                null,
                null,
                "끼움 대학교",
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 6, 30)));

    assertThatThrownBy(() -> experienceService.update(userId, 10L, request))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(EXPERIENCE_EDUCATION_NAME_TOO_LONG);
  }

  @Test
  @DisplayName("ETC 수정 시 공통 필드와 etc detail이 업데이트된다")
  void updateEtcExperience() {
    Long userId = 1L;
    User owner = createUser(userId);
    Experience experience = createExperience(owner, PieceType.ETC);
    setId(experience, 10L);
    Etc etc = createEtc(experience);

    when(experienceRepository.findByIdWithPiece(10L)).thenReturn(Optional.of(experience));
    when(tagRepository.findByExperienceId(10L)).thenReturn(List.of());
    when(etcRepository.findByExperienceId(10L)).thenReturn(Optional.of(etc));

    ExperienceUpdateRequest request = etcRequest("수정된 제목");
    experienceService.update(userId, 10L, request);

    assertThat(experience.getTitle()).isEqualTo("수정된 제목");
    verify(jdExperienceAnalysisService).evictCache(10L);
  }

  @Test
  @DisplayName("수정 시 기존 태그를 전체 삭제하고 새 태그를 저장한다")
  void deleteOldTagsAndSaveNewTags() {
    Long userId = 1L;
    User owner = createUser(userId);
    Experience experience = createExperience(owner, PieceType.ACTIVITY);
    setId(experience, 10L);
    Activity activity = createActivity(experience);

    when(experienceRepository.findByIdWithPiece(10L)).thenReturn(Optional.of(experience));
    when(tagRepository.findByExperienceId(10L)).thenReturn(List.of());
    when(activityRepository.findByExperienceId(10L)).thenReturn(Optional.of(activity));

    ExperienceUpdateRequest request = activityRequest("제목");
    experienceService.update(userId, 10L, request);

    verify(tagRepository).deleteAll(any());
    verify(tagRepository).saveAll(any());
  }

  // ── 캐시 무효화 ───────────────────────────────────────────────

  @Test
  @DisplayName("수정 성공 시 Redis 캐시를 무효화한다")
  void evictCacheAfterUpdate() {
    Long userId = 1L;
    User owner = createUser(userId);
    Experience experience = createExperience(owner, PieceType.ACTIVITY);
    setId(experience, 10L);
    Activity activity = createActivity(experience);

    when(experienceRepository.findByIdWithPiece(10L)).thenReturn(Optional.of(experience));
    when(tagRepository.findByExperienceId(10L)).thenReturn(List.of());
    when(activityRepository.findByExperienceId(10L)).thenReturn(Optional.of(activity));

    experienceService.update(userId, 10L, activityRequest("제목"));

    verify(jdExperienceAnalysisService).evictCache(10L);
  }

  @Test
  @DisplayName("권한 검증 실패 시 캐시 무효화를 호출하지 않는다")
  void notEvictCacheWhenForbidden() {
    Long ownerId = 1L;
    Long requestUserId = 2L;
    User owner = createUser(ownerId);
    Experience experience = createExperience(owner, PieceType.ACTIVITY);
    setId(experience, 10L);

    when(experienceRepository.findByIdWithPiece(10L)).thenReturn(Optional.of(experience));

    assertThatThrownBy(() -> experienceService.update(requestUserId, 10L, activityRequest("제목")))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(FORBIDDEN);

    verify(jdExperienceAnalysisService, never()).evictCache(any());
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

  private Experience createExperience(User owner, PieceType type) {
    Piece piece = Piece.builder().type(type).user(owner).build();
    return Experience.builder()
        .piece(piece)
        .title("기존 제목")
        .oneLineIntro("기존 한 줄 소개")
        .situation("S")
        .task("T")
        .act("A")
        .result("R")
        .taken("L")
        .build();
  }

  private Activity createActivity(Experience experience) {
    return Activity.builder()
        .name("기존 동아리")
        .teamNum(5)
        .role("기존 역할")
        .contributionRate(80)
        .startDate(LocalDate.of(2023, 1, 1))
        .endDate(LocalDate.of(2023, 6, 30))
        .experience(experience)
        .build();
  }

  private Career createCareer(Experience experience) {
    return Career.builder()
        .name("기존 제목")
        .company("기존 회사")
        .employmentStatus("인턴")
        .startDate(LocalDate.of(2023, 1, 1))
        .endDate(LocalDate.of(2023, 6, 30))
        .experience(experience)
        .build();
  }

  private Education createEducation(Experience experience) {
    return Education.builder()
        .organizationName("기존 대학교")
        .name("기존 강의명")
        .startDate(LocalDate.of(2023, 1, 1))
        .endDate(LocalDate.of(2023, 6, 30))
        .experience(experience)
        .build();
  }

  private Etc createEtc(Experience experience) {
    return Etc.builder()
        .startDate(LocalDate.of(2023, 1, 1))
        .endDate(LocalDate.of(2023, 6, 30))
        .experience(experience)
        .build();
  }

  private ExperienceUpdateRequest activityRequest(String title) {
    return new ExperienceUpdateRequest(
        title,
        "한 줄 소개",
        List.of(new TagCreateRequest(TagCategory.TECH, "Java")),
        "S",
        "T",
        "A",
        "R",
        "L",
        new Detail(
            "끼움 동아리",
            5,
            "백엔드",
            80,
            null,
            null,
            null,
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 6, 30)));
  }

  private ExperienceUpdateRequest careerRequest(String title) {
    return new ExperienceUpdateRequest(
        title,
        "한 줄 소개",
        List.of(new TagCreateRequest(TagCategory.TECH, "Spring")),
        "S",
        "T",
        "A",
        "R",
        "L",
        new Detail(
            null,
            null,
            null,
            null,
            "끼움 회사",
            "인턴",
            null,
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 6, 30)));
  }

  private ExperienceUpdateRequest educationRequest(String title) {
    return new ExperienceUpdateRequest(
        title,
        "한 줄 소개",
        List.of(new TagCreateRequest(TagCategory.COMPETENCY, "문제해결")),
        "S",
        "T",
        "A",
        "R",
        "L",
        new Detail(
            "스프링 기초",
            null,
            null,
            null,
            null,
            null,
            "끼움 대학교",
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 6, 30)));
  }

  private ExperienceUpdateRequest etcRequest(String title) {
    return new ExperienceUpdateRequest(
        title,
        "한 줄 소개",
        List.of(),
        "S",
        "T",
        "A",
        "R",
        "L",
        new Detail(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 6, 30)));
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
