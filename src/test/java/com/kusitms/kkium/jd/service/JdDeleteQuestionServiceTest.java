package com.kusitms.kkium.jd.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.jd.repository.JdAnswerRepository;
import com.kusitms.kkium.jd.repository.JdQuestionRepository;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.resume.repository.AnswerExperienceRepository;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class JdDeleteQuestionServiceTest {

  @Mock private JdRepository jdRepository;
  @Mock private JdQuestionRepository jdQuestionRepository;
  @Mock private JdAnswerRepository jdAnswerRepository;
  @Mock private AnswerExperienceRepository answerExperienceRepository;
  @Mock private UserRepository userRepository;
  @Mock private JdExperienceAnalysisService jdExperienceAnalysisService;

  private JdService jdService;

  private static final Long USER_ID = 1L;
  private static final Long OTHER_USER_ID = 2L;
  private static final Long JD_ID = 10L;
  private static final Long QUESTION_ID = 100L;

  @BeforeEach
  void setUp() {
    jdService =
        new JdService(
            jdRepository,
            jdQuestionRepository,
            jdAnswerRepository,
            answerExperienceRepository,
            userRepository,
            jdExperienceAnalysisService);
  }

  @Test
  @DisplayName("자기소개서 문항 정상 삭제: 관련 답변·경험·문항이 순서대로 삭제되고 orderNum이 재정렬된다")
  void deleteQuestion_정상_삭제() {
    Jd jd = mock(Jd.class);
    User owner = mock(User.class);
    JdQuestion question = mock(JdQuestion.class);

    when(jdRepository.findByIdAndDeleteAtIsNull(JD_ID)).thenReturn(Optional.of(jd));
    when(jd.getUser()).thenReturn(owner);
    when(owner.getId()).thenReturn(USER_ID);
    when(jdQuestionRepository.findByIdAndJdId(QUESTION_ID, JD_ID))
        .thenReturn(Optional.of(question));
    when(question.getOrderNum()).thenReturn(2);

    jdService.deleteQuestion(JD_ID, QUESTION_ID, USER_ID);

    InOrder inOrder =
        Mockito.inOrder(answerExperienceRepository, jdAnswerRepository, jdQuestionRepository);
    inOrder.verify(answerExperienceRepository).deleteAllByJdQuestion(question);
    inOrder.verify(jdAnswerRepository).deleteAllByJdQuestion(question);
    inOrder.verify(jdQuestionRepository).delete(question);
    inOrder.verify(jdQuestionRepository).decrementOrderNumAfter(JD_ID, 2);
  }

  @Test
  @DisplayName("자기소개서 문항 삭제: JD가 존재하지 않으면 JD_NOT_FOUND 예외를 던진다")
  void deleteQuestion_JD_없음() {
    when(jdRepository.findByIdAndDeleteAtIsNull(JD_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> jdService.deleteQuestion(JD_ID, QUESTION_ID, USER_ID))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.JD_NOT_FOUND);
  }

  @Test
  @DisplayName("자기소개서 문항 삭제: 타인 소유 JD이면 FORBIDDEN 예외를 던진다")
  void deleteQuestion_타인_소유_FORBIDDEN() {
    Jd jd = mock(Jd.class);
    User owner = mock(User.class);

    when(jdRepository.findByIdAndDeleteAtIsNull(JD_ID)).thenReturn(Optional.of(jd));
    when(jd.getUser()).thenReturn(owner);
    when(owner.getId()).thenReturn(OTHER_USER_ID);

    assertThatThrownBy(() -> jdService.deleteQuestion(JD_ID, QUESTION_ID, USER_ID))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.FORBIDDEN);
  }

  @Test
  @DisplayName("자기소개서 문항 삭제: 존재하지 않는 문항이면 INVALID_QUESTION_FOR_JD 예외를 던진다")
  void deleteQuestion_없는_문항() {
    Jd jd = mock(Jd.class);
    User owner = mock(User.class);

    when(jdRepository.findByIdAndDeleteAtIsNull(JD_ID)).thenReturn(Optional.of(jd));
    when(jd.getUser()).thenReturn(owner);
    when(owner.getId()).thenReturn(USER_ID);
    when(jdQuestionRepository.findByIdAndJdId(QUESTION_ID, JD_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> jdService.deleteQuestion(JD_ID, QUESTION_ID, USER_ID))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_QUESTION_FOR_JD);
  }
}
