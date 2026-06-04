package com.kusitms.kkium.resume.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;
import com.kusitms.kkium.jd.domain.JdAnswer;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.jd.repository.JdAnswerRepository;
import com.kusitms.kkium.jd.repository.JdQuestionRepository;
import com.kusitms.kkium.resume.dto.request.ResumeAnswerSaveRequest;
import com.kusitms.kkium.resume.repository.AnswerExperienceRepository;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ResumeAnswerServiceTest {

  @Mock private JdAnswerRepository jdAnswerRepository;
  @Mock private JdQuestionRepository jdQuestionRepository;
  @Mock private AnswerExperienceRepository answerExperienceRepository;
  @Mock private UserRepository userRepository;

  private ResumeAnswerService resumeAnswerService;

  private static final Long USER_ID = 1L;
  private static final Long JD_ID = 10L;
  private static final Long QUESTION_ID = 100L;

  @BeforeEach
  void setUp() {
    resumeAnswerService =
        new ResumeAnswerService(
            jdAnswerRepository, jdQuestionRepository, answerExperienceRepository, userRepository);
  }

  @Test
  @DisplayName("신규 답변 저장: 기존 답변이 없으면 새 JdAnswer를 INSERT한다")
  void saveAnswers_신규_저장() {
    User user = mock(User.class);
    JdQuestion question = mock(JdQuestion.class);
    JdAnswer savedAnswer = mock(JdAnswer.class);

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(question.getId()).thenReturn(QUESTION_ID);
    when(jdQuestionRepository.findAllByIdInAndJdId(List.of(QUESTION_ID), JD_ID))
        .thenReturn(List.of(question));
    when(jdAnswerRepository.findAllByJdQuestionInAndUser(anyList(), any(User.class)))
        .thenReturn(List.of()); // 기존 답변 없음
    when(jdAnswerRepository.save(any(JdAnswer.class))).thenReturn(savedAnswer);
    when(savedAnswer.getId()).thenReturn(200L);
    when(savedAnswer.getJdQuestion()).thenReturn(question);

    ResumeAnswerSaveRequest request =
        new ResumeAnswerSaveRequest(
            List.of(new ResumeAnswerSaveRequest.AnswerRequest(QUESTION_ID, "답변 내용", List.of())));

    resumeAnswerService.saveAnswers(JD_ID, USER_ID, request);

    verify(jdAnswerRepository).save(any(JdAnswer.class));
    verify(answerExperienceRepository).deleteAllByJdAnswerIdIn(List.of(200L));
  }

  @Test
  @DisplayName("기존 답변 업데이트: 이미 답변이 있으면 INSERT 없이 내용을 업데이트한다")
  void saveAnswers_기존_업데이트() {
    User user = mock(User.class);
    JdQuestion question = mock(JdQuestion.class);
    JdAnswer existingAnswer = mock(JdAnswer.class);

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(question.getId()).thenReturn(QUESTION_ID);
    when(jdQuestionRepository.findAllByIdInAndJdId(List.of(QUESTION_ID), JD_ID))
        .thenReturn(List.of(question));
    when(jdAnswerRepository.findAllByJdQuestionInAndUser(anyList(), any(User.class)))
        .thenReturn(List.of(existingAnswer)); // 기존 답변 있음
    when(existingAnswer.getJdQuestion()).thenReturn(question);
    when(existingAnswer.getId()).thenReturn(200L);

    ResumeAnswerSaveRequest request =
        new ResumeAnswerSaveRequest(
            List.of(new ResumeAnswerSaveRequest.AnswerRequest(QUESTION_ID, "수정된 답변", List.of())));

    resumeAnswerService.saveAnswers(JD_ID, USER_ID, request);

    verify(existingAnswer).updateContent("수정된 답변");
    verify(jdAnswerRepository, never()).save(any(JdAnswer.class));
  }

  @Test
  @DisplayName("다른 JD의 문항에 접근하면 INVALID_QUESTION_FOR_JD 예외를 던진다")
  void saveAnswers_잘못된_JD_문항() {
    User user = mock(User.class);

    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    // questionId가 해당 jdId에 속하지 않아 조회되지 않음 (요청은 1개, 조회는 0개)
    when(jdQuestionRepository.findAllByIdInAndJdId(anyList(), any(Long.class)))
        .thenReturn(List.of()); // 0개 반환 → size 불일치

    ResumeAnswerSaveRequest request =
        new ResumeAnswerSaveRequest(
            List.of(new ResumeAnswerSaveRequest.AnswerRequest(QUESTION_ID, "답변", List.of())));

    assertThatThrownBy(() -> resumeAnswerService.saveAnswers(JD_ID, USER_ID, request))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.INVALID_QUESTION_FOR_JD);
  }

  @Test
  @DisplayName("존재하지 않는 사용자이면 USER_NOT_FOUND 예외를 던진다")
  void saveAnswers_사용자_없음() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    ResumeAnswerSaveRequest request =
        new ResumeAnswerSaveRequest(
            List.of(new ResumeAnswerSaveRequest.AnswerRequest(QUESTION_ID, "답변", List.of())));

    assertThatThrownBy(() -> resumeAnswerService.saveAnswers(JD_ID, USER_ID, request))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.USER_NOT_FOUND);
  }
}
