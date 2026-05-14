package com.kusitms.kkium.jd.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.JD_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_NOT_FOUND;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdAnswer;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.jd.dto.request.JdOrderUpdateRequest;
import com.kusitms.kkium.jd.dto.request.JdSaveRequest;
import com.kusitms.kkium.jd.dto.request.JdTitleUpdateRequest;
import com.kusitms.kkium.jd.dto.request.JdUpdateRequest;
import com.kusitms.kkium.jd.dto.response.JdAnalysisResponse;
import com.kusitms.kkium.jd.dto.response.JdListPageResponse;
import com.kusitms.kkium.jd.dto.response.JdListResponse;
import com.kusitms.kkium.jd.dto.response.JdQuestionResponse;
import com.kusitms.kkium.jd.dto.response.JdResponse;
import com.kusitms.kkium.jd.dto.response.JdSaveResponse;
import com.kusitms.kkium.jd.repository.JdAnswerRepository;
import com.kusitms.kkium.jd.repository.JdQuestionRepository;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;
import com.kusitms.kkium.user.utils.CustomUserDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JdService {

  private static final int MAX_TARGET_COUNT = 5;

  private final JdRepository jdRepository;
  private final JdQuestionRepository jdQuestionRepository;
  private final JdAnswerRepository jdAnswerRepository;
  private final UserRepository userRepository;

  public JdListPageResponse getJdList(CustomUserDetails userDetails, int page, int size) {
    Long userId = userDetails.getId();
    Pageable pageable = PageRequest.of(page, size);
    Page<JdListResponse> result =
        jdRepository.findByUserIdAndDeleteAtIsNull(userId, pageable).map(JdListResponse::from);

    return new JdListPageResponse(
        result.getContent(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages(),
        result.hasNext());
  }

  @Transactional
  public void toggleTarget(Long jdId, CustomUserDetails userDetails) {
    Long userId = userDetails.getId();

    Jd jd =
        jdRepository
            .findByIdAndDeleteAtIsNull(jdId)
            .orElseThrow(() -> new BaseException(ErrorCode.JD_NOT_FOUND));

    // 5개 제한 체크
    if (!Boolean.TRUE.equals(jd.getIsTarget())) {
      long targetCount = jdRepository.countByUserIdAndIsTargetTrueAndDeleteAtIsNull(userId);
      if (targetCount >= MAX_TARGET_COUNT) {
        throw new BaseException(ErrorCode.JD_TARGET_LIMIT_EXCEEDED);
      }
    }

    jd.toggleTarget();
  }

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
  public JdAnalysisResponse getJdAnalysis(Long jdId) {
    return JdAnalysisResponse.from(findJdById(jdId));
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

  @Transactional
  public void updateOrder(JdOrderUpdateRequest request, CustomUserDetails userDetails) {
    Long userId = userDetails.getId();
    List<Long> jdIds = request.jdIds();

    List<Jd> jds = jdRepository.findAllByIdInAndDeleteAtIsNull(jdIds);

    jds.forEach(
        jd -> {
          if (!jd.getUser().getId().equals(userId)) {
            throw new BaseException(ErrorCode.FORBIDDEN);
          }
        });

    for (int i = 0; i < jdIds.size(); i++) {
      final int order = i + 1;
      final Long jdId = jdIds.get(i);
      jds.stream()
          .filter(jd -> jd.getId().equals(jdId))
          .findFirst()
          .ifPresent(jd -> jd.updateSortOrder(order));
    }
  }

  @Transactional
  public void deleteJd(Long jdId, CustomUserDetails userDetails) {
    Long userId = userDetails.getId();

    Jd jd =
        jdRepository
            .findByIdAndDeleteAtIsNull(jdId)
            .orElseThrow(() -> new BaseException(ErrorCode.JD_NOT_FOUND));

    if (!jd.getUser().getId().equals(userId)) {
      throw new BaseException(ErrorCode.FORBIDDEN);
    }

    jd.delete();
  }

  @Transactional
  public void updateTitle(Long jdId, JdTitleUpdateRequest request, CustomUserDetails userDetails) {
    Long userId = userDetails.getId();

    Jd jd =
        jdRepository
            .findByIdAndDeleteAtIsNull(jdId)
            .orElseThrow(() -> new BaseException(ErrorCode.JD_NOT_FOUND));

    if (!jd.getUser().getId().equals(userId)) {
      throw new BaseException(ErrorCode.FORBIDDEN);
    }

    jd.updateTitle(request.title());
  }
}
