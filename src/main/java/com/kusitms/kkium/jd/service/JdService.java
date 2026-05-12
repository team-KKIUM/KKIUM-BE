package com.kusitms.kkium.jd.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.JD_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_NOT_FOUND;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdAnswer;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.jd.dto.request.JdUpdateRequest;
import com.kusitms.kkium.jd.dto.response.JdQuestionResponse;
import com.kusitms.kkium.jd.dto.response.JdResponse;
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
}
