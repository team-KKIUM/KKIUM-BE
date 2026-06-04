package com.kusitms.kkium.experience.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.domain.ExperienceOrder;
import com.kusitms.kkium.experience.domain.Piece;
import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.experience.dto.request.ExperienceOrderUpdateRequest;
import com.kusitms.kkium.experience.dto.response.ExperienceDetailResponse;
import com.kusitms.kkium.experience.dto.response.ExperienceListResponse;
import com.kusitms.kkium.experience.repository.ActivityRepository;
import com.kusitms.kkium.experience.repository.CareerRepository;
import com.kusitms.kkium.experience.repository.EducationRepository;
import com.kusitms.kkium.experience.repository.EtcRepository;
import com.kusitms.kkium.experience.repository.ExperienceOrderRepository;
import com.kusitms.kkium.experience.repository.ExperienceRepository;
import com.kusitms.kkium.experience.repository.PieceRepository;
import com.kusitms.kkium.experience.repository.TagRepository;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;
import com.kusitms.kkium.home.service.JobTypeUpdateService;
import com.kusitms.kkium.jd.service.JdExperienceAnalysisService;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExperienceServiceTest {

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

  @InjectMocks private ExperienceService experienceService;

  private static final Long USER_ID = 1L;
  private static final Long OTHER_USER_ID = 2L;
  private static final Long EXPERIENCE_ID = 10L;

  // ── getDetail ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("경험 상세 조회: 본인 소유 경험이면 상세 정보를 반환한다")
  void getDetail_정상() {
    Experience experience = mock(Experience.class);
    Piece piece = mock(Piece.class);
    User user = mock(User.class);

    when(experienceRepository.findByIdWithPiece(EXPERIENCE_ID)).thenReturn(Optional.of(experience));
    when(experience.getPiece()).thenReturn(piece);
    when(piece.getUser()).thenReturn(user);
    when(user.getId()).thenReturn(USER_ID);
    when(piece.getType()).thenReturn(PieceType.ACTIVITY);
    when(experience.getId()).thenReturn(EXPERIENCE_ID);
    when(tagRepository.findByExperienceIdIn(anyList())).thenReturn(List.of());
    when(activityRepository.findByExperienceId(EXPERIENCE_ID)).thenReturn(Optional.empty());

    ExperienceDetailResponse result = experienceService.getDetail(USER_ID, EXPERIENCE_ID);

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("경험 상세 조회: 타인 소유 경험이면 FORBIDDEN 예외를 던진다")
  void getDetail_타인_소유_FORBIDDEN() {
    Experience experience = mock(Experience.class);
    Piece piece = mock(Piece.class);
    User owner = mock(User.class);

    when(experienceRepository.findByIdWithPiece(EXPERIENCE_ID)).thenReturn(Optional.of(experience));
    when(experience.getPiece()).thenReturn(piece);
    when(piece.getUser()).thenReturn(owner);
    when(owner.getId()).thenReturn(OTHER_USER_ID);

    assertThatThrownBy(() -> experienceService.getDetail(USER_ID, EXPERIENCE_ID))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("경험 상세 조회: 존재하지 않는 경험 ID이면 EXPERIENCE_NOT_FOUND 예외를 던진다")
  void getDetail_존재하지않는_ID() {
    when(experienceRepository.findByIdWithPiece(EXPERIENCE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> experienceService.getDetail(USER_ID, EXPERIENCE_ID))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.EXPERIENCE_NOT_FOUND);
  }

  // ── getList ──────────────────────────────────────────────────────────────

  @Test
  @DisplayName("경험 목록 조회: hasNext=false이면 nextCursor가 null이다")
  void getList_hasNext_false() {
    Experience exp = mock(Experience.class);
    Piece piece = mock(Piece.class);

    when(exp.getId()).thenReturn(EXPERIENCE_ID);
    when(exp.getPiece()).thenReturn(piece);
    when(piece.getType()).thenReturn(PieceType.ACTIVITY);
    when(experienceRepository.findAllByUserId(eq(USER_ID), any(Pageable.class)))
        .thenReturn(List.of(exp));
    when(tagRepository.findByExperienceIdIn(anyList())).thenReturn(List.of());
    when(experiencePeriodResolver.resolvePeriodBulk(anyList())).thenReturn(Map.of());
    when(experienceOrderRepository.findAllByUserIdAndPieceTypeAndExperienceIdIn(
            eq(USER_ID), eq(PieceType.ALL), anyList()))
        .thenReturn(List.of());

    ExperienceListResponse result =
        experienceService.getList(USER_ID, PieceType.ALL, null, 10, null);

    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursor()).isNull();
    assertThat(result.experiences()).hasSize(1);
  }

  @Test
  @DisplayName("경험 목록 조회: 결과가 size+1이면 hasNext=true이고 nextCursor가 존재한다")
  void getList_hasNext_true() {
    List<Experience> exps =
        List.of(
            mockExperience(10L, PieceType.ALL),
            mockExperience(11L, PieceType.ALL),
            mockExperience(12L, PieceType.ALL)); // size=2, 3개 반환 → hasNext=true

    when(experienceRepository.findAllByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(exps);
    when(tagRepository.findByExperienceIdIn(anyList())).thenReturn(List.of());
    when(experiencePeriodResolver.resolvePeriodBulk(anyList())).thenReturn(Map.of());

    ExperienceOrder order = mock(ExperienceOrder.class);
    Experience lastExp = exps.get(1); // size=2이면 index 1이 마지막 content
    when(order.getExperience()).thenReturn(lastExp);
    when(order.getSortOrder()).thenReturn(5);
    when(experienceOrderRepository.findAllByUserIdAndPieceTypeAndExperienceIdIn(
            eq(USER_ID), eq(PieceType.ALL), anyList()))
        .thenReturn(List.of(order));

    ExperienceListResponse result =
        experienceService.getList(USER_ID, PieceType.ALL, null, 2, null);

    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursor()).isEqualTo(5);
    assertThat(result.experiences()).hasSize(2);
  }

  @Test
  @DisplayName("경험 목록 조회: 결과가 없으면 빈 리스트를 반환한다")
  void getList_빈_결과() {
    when(experienceRepository.findAllByUserId(eq(USER_ID), any(Pageable.class)))
        .thenReturn(List.of());
    when(tagRepository.findByExperienceIdIn(anyList())).thenReturn(List.of());
    when(experiencePeriodResolver.resolvePeriodBulk(anyList())).thenReturn(Map.of());
    when(experienceOrderRepository.findAllByUserIdAndPieceTypeAndExperienceIdIn(
            eq(USER_ID), eq(PieceType.ALL), anyList()))
        .thenReturn(List.of());

    ExperienceListResponse result =
        experienceService.getList(USER_ID, PieceType.ALL, null, 10, null);

    assertThat(result.hasNext()).isFalse();
    assertThat(result.experiences()).isEmpty();
  }

  // ── updateOrder ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("경험 카드 재배열: 정상적으로 sort_order가 업데이트된다")
  void updateOrder_정상() {
    List<Long> experienceIds = List.of(10L, 11L);
    ExperienceOrderUpdateRequest request =
        new ExperienceOrderUpdateRequest(PieceType.ALL, experienceIds);

    Experience exp1 = mock(Experience.class);
    Experience exp2 = mock(Experience.class);
    when(exp1.getId()).thenReturn(10L);
    when(exp2.getId()).thenReturn(11L);

    ExperienceOrder order1 = mock(ExperienceOrder.class);
    ExperienceOrder order2 = mock(ExperienceOrder.class);
    User user = mock(User.class);
    when(user.getId()).thenReturn(USER_ID);
    when(order1.getExperience()).thenReturn(exp1);
    when(order2.getExperience()).thenReturn(exp2);
    when(order1.getUser()).thenReturn(user);
    when(order2.getUser()).thenReturn(user);

    when(experienceOrderRepository.findAllByUserIdAndPieceTypeAndExperienceIdIn(
            USER_ID, PieceType.ALL, experienceIds))
        .thenReturn(List.of(order1, order2));

    experienceService.updateOrder(request, USER_ID);

    verify(order1).updateSortOrder(1);
    verify(order2).updateSortOrder(2);
  }

  @Test
  @DisplayName("경험 카드 재배열: 요청한 ID 중 존재하지 않는 order가 있으면 EXPERIENCE_ORDER_NOT_FOUND 예외를 던진다")
  void updateOrder_없는_order() {
    List<Long> experienceIds = List.of(10L, 11L);
    ExperienceOrderUpdateRequest request =
        new ExperienceOrderUpdateRequest(PieceType.ALL, experienceIds);

    when(experienceOrderRepository.findAllByUserIdAndPieceTypeAndExperienceIdIn(
            USER_ID, PieceType.ALL, experienceIds))
        .thenReturn(List.of(mock(ExperienceOrder.class))); // 1개만 반환 → size 불일치

    assertThatThrownBy(() -> experienceService.updateOrder(request, USER_ID))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.EXPERIENCE_ORDER_NOT_FOUND);
  }

  @Test
  @DisplayName("경험 검색: keyword가 있으면 findIdsByKeyword + findAllByIdIn 2-step으로 조회한다")
  void getList_키워드_검색() {
    String keyword = "테스트키워드";
    Experience exp = mockExperience(EXPERIENCE_ID, PieceType.ALL);

    when(experienceRepository.findIdsByKeyword(eq(USER_ID), eq(keyword), any(Pageable.class)))
        .thenReturn(List.of(EXPERIENCE_ID));
    when(experienceRepository.findAllByIdIn(List.of(EXPERIENCE_ID))).thenReturn(List.of(exp));
    when(tagRepository.findByExperienceIdIn(anyList())).thenReturn(List.of());
    when(experiencePeriodResolver.resolvePeriodBulk(anyList())).thenReturn(Map.of());
    when(experienceOrderRepository.findAllByUserIdAndPieceTypeAndExperienceIdIn(
            eq(USER_ID), eq(PieceType.ALL), anyList()))
        .thenReturn(List.of());

    ExperienceListResponse result =
        experienceService.getList(USER_ID, PieceType.ALL, null, 10, keyword);

    assertThat(result.experiences()).hasSize(1);
    verify(experienceRepository).findIdsByKeyword(eq(USER_ID), eq(keyword), any(Pageable.class));
    verify(experienceRepository).findAllByIdIn(List.of(EXPERIENCE_ID));
  }

  @Test
  @DisplayName("경험 검색: keyword가 빈 문자열이면 findAllByUserId를 호출한다")
  void getList_빈_키워드() {
    Experience exp = mockExperience(EXPERIENCE_ID, PieceType.ALL);

    when(experienceRepository.findAllByUserId(eq(USER_ID), any(Pageable.class)))
        .thenReturn(List.of(exp));
    when(tagRepository.findByExperienceIdIn(anyList())).thenReturn(List.of());
    when(experiencePeriodResolver.resolvePeriodBulk(anyList())).thenReturn(Map.of());
    when(experienceOrderRepository.findAllByUserIdAndPieceTypeAndExperienceIdIn(
            eq(USER_ID), eq(PieceType.ALL), anyList()))
        .thenReturn(List.of());

    ExperienceListResponse result = experienceService.getList(USER_ID, PieceType.ALL, null, 10, "");

    assertThat(result.experiences()).hasSize(1);
    verify(experienceRepository).findAllByUserId(eq(USER_ID), any(Pageable.class));
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private Experience mockExperience(Long id, PieceType type) {
    Experience exp = mock(Experience.class);
    Piece piece = mock(Piece.class);
    when(exp.getId()).thenReturn(id);
    when(exp.getPiece()).thenReturn(piece);
    when(piece.getType()).thenReturn(type);
    return exp;
  }
}
