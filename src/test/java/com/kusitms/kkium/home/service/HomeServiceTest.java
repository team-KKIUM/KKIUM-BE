package com.kusitms.kkium.home.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;
import com.kusitms.kkium.home.dto.response.HomeResponse;
import com.kusitms.kkium.home.repository.HomeRepository;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

  @Mock private HomeRepository homeRepository;
  @Mock private UserRepository userRepository;

  private HomeService homeService;

  private static final Long USER_ID = 1L;

  @BeforeEach
  void setUp() {
    homeService = new HomeService(homeRepository, userRepository);
  }

  @Test
  @DisplayName("홈 대시보드 정상 조회: targetJd가 있으면 정보가 포함된 응답을 반환한다")
  void getHome_정상_조회() {
    User user = mock(User.class);
    Jd jd = mock(Jd.class);

    List<Object[]> rawCounts = new ArrayList<>();
    rawCounts.add(new Object[] {PieceType.ACTIVITY, 5L});

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(user.getJobType()).thenReturn(null);
    when(homeRepository.findTargetJds(USER_ID)).thenReturn(List.of(jd));
    when(homeRepository.countTotalExperience(USER_ID)).thenReturn(5);
    when(homeRepository.countThisMonthExperience(eq(USER_ID), any(), any()))
        .thenReturn(3, 2); // 이번달=3, 지난달=2
    when(homeRepository.countByPieceType(USER_ID, PieceType.ALL)).thenReturn(rawCounts);

    HomeResponse result = homeService.getHome(USER_ID);

    assertThat(result).isNotNull();
    assertThat(result.targetJds()).hasSize(1);
    assertThat(result.totalExperienceCount()).isEqualTo(5);
    assertThat(result.thisMonthExperienceCount()).isEqualTo(3);
    assertThat(result.lastMonthDiff()).isEqualTo(1); // 3 - 2
  }

  @Test
  @DisplayName("홈 대시보드 조회: targetJd가 없으면 targetJds가 빈 리스트로 반환된다")
  void getHome_targetJd_없음() {
    User user = mock(User.class);

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(user.getJobType()).thenReturn(null);
    when(homeRepository.findTargetJds(USER_ID)).thenReturn(List.of());
    when(homeRepository.countTotalExperience(USER_ID)).thenReturn(0);
    when(homeRepository.countThisMonthExperience(eq(USER_ID), any(), any())).thenReturn(0, 0);
    when(homeRepository.countByPieceType(USER_ID, PieceType.ALL)).thenReturn(new ArrayList<>());

    HomeResponse result = homeService.getHome(USER_ID);

    assertThat(result.targetJds()).isEmpty();
    assertThat(result.totalExperienceCount()).isZero();
  }

  @Test
  @DisplayName("홈 대시보드 조회: 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
  void getHome_사용자_없음() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> homeService.getHome(USER_ID))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
  }

  @Test
  @DisplayName("홈 대시보드 조회: jobType이 null이면 JobTypeInfo의 typeName도 null이다")
  void getHome_jobType_없음() {
    User user = mock(User.class);

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(user.getJobType()).thenReturn(null);
    when(homeRepository.findTargetJds(USER_ID)).thenReturn(List.of());
    when(homeRepository.countTotalExperience(USER_ID)).thenReturn(0);
    when(homeRepository.countThisMonthExperience(eq(USER_ID), any(), any())).thenReturn(0, 0);
    when(homeRepository.countByPieceType(USER_ID, PieceType.ALL)).thenReturn(new ArrayList<>());

    HomeResponse result = homeService.getHome(USER_ID);

    assertThat(result.jobType().typeName()).isNull();
  }

  @Test
  @DisplayName("홈 대시보드 조회: 이번달 경험이 지난달보다 적으면 lastMonthDiff가 음수다")
  void getHome_lastMonthDiff_음수() {
    User user = mock(User.class);

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(user.getJobType()).thenReturn(null);
    when(homeRepository.findTargetJds(USER_ID)).thenReturn(List.of());
    when(homeRepository.countTotalExperience(USER_ID)).thenReturn(0);
    when(homeRepository.countThisMonthExperience(eq(USER_ID), any(), any()))
        .thenReturn(1, 5); // 이번달=1, 지난달=5
    when(homeRepository.countByPieceType(USER_ID, PieceType.ALL)).thenReturn(new ArrayList<>());

    HomeResponse result = homeService.getHome(USER_ID);

    assertThat(result.lastMonthDiff()).isEqualTo(-4); // 1 - 5
  }
}
