package com.kusitms.kkium.jd.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.JD_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_NOT_FOUND;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdAnswer;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.jd.dto.request.JdSaveRequest;
import com.kusitms.kkium.jd.dto.request.JdUpdateRequest;
import com.kusitms.kkium.jd.dto.response.JdQuestionResponse;
import com.kusitms.kkium.jd.dto.response.JdResponse;
import com.kusitms.kkium.jd.dto.response.JdSaveResponse;
import com.kusitms.kkium.jd.repository.JdAnswerRepository;
import com.kusitms.kkium.jd.repository.JdQuestionRepository;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JdService {

  private final JdRepository jdRepository;
  private final JdQuestionRepository jdQuestionRepository;
  private final JdAnswerRepository jdAnswerRepository;
  private final UserRepository userRepository;

  @Transactional
  public JdSaveResponse saveJd(Long userId, JdSaveRequest request) {
    User user = findUserById(userId);

    Jd jd =
        jdRepository.save(
            Jd.builder()
                .user(user)
                .linkUrl(request.url())
                .postingTitle(request.postingTitle())
                .companyName(request.companyName())
                .recruitmentField(request.recruitmentField())
                .startDate(parseDate(request.startDate()))
                .endDate(parseDate(request.endDate()))
                .rawText(request.content())
                .build());

    if (request.questions() != null) {
      for (int i = 0; i < request.questions().size(); i++) {
        jdQuestionRepository.save(
            JdQuestion.builder()
                .jd(jd)
                .orderNum(i + 1)
                .content(request.questions().get(i))
                .build());
      }
    }

    return new JdSaveResponse(jd.getId());
  }

  @Transactional(readOnly = true)
  public JdResponse getJd(Long jdId, Long userId) {
    Jd jd = findJdById(jdId);
    User user = findUserById(userId);

    List<JdQuestionResponse> questionResponses =
        jdQuestionRepository.findByJdOrderByOrderNum(jd).stream()
            .map(
                question ->
                    JdQuestionResponse.from(
                        question,
                        jdAnswerRepository.findByJdQuestionAndUser(question, user).orElse(null)))
            .toList();

    return JdResponse.from(jd, questionResponses);
  }

  @Transactional
  public void updateJd(Long jdId, Long userId, JdUpdateRequest request) {
    Jd jd = findJdById(jdId);
    User user = findUserById(userId);

    jd.update(
        request.postingTitle(),
        request.companyName(),
        request.recruitmentField(),
        request.startDate(),
        request.endDate());

    if (request.questions() == null) return;

    request.questions().forEach(q -> updateQuestion(q, user));
  }

  private void updateQuestion(JdUpdateRequest.QuestionUpdateRequest q, User user) {
    JdQuestion question = findQuestionById(q.questionId());
    question.updateContent(q.content());

    JdAnswer answer =
        jdAnswerRepository
            .findByJdQuestionAndUser(question, user)
            .orElseGet(
                () ->
                    jdAnswerRepository.save(
                        JdAnswer.builder()
                            .jdQuestion(question)
                            .user(user)
                            .content(q.answer())
                            .build()));
    answer.updateContent(q.answer());
  }

  private Jd findJdById(Long jdId) {
    return jdRepository.findById(jdId).orElseThrow(() -> new BaseException(JD_NOT_FOUND));
  }

  private JdQuestion findQuestionById(Long questionId) {
    return jdQuestionRepository
        .findById(questionId)
        .orElseThrow(() -> new BaseException(JD_NOT_FOUND));
  }

  private User findUserById(Long userId) {
    return userRepository.findById(userId).orElseThrow(() -> new BaseException(USER_NOT_FOUND));
  }

  private LocalDateTime parseDate(String date) {
    if (date == null || date.isBlank()) return null;
    try {
      return LocalDate.parse(date).atStartOfDay();
    } catch (Exception e) {
      return null;
    }
  }
}
