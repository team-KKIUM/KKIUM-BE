package com.kusitms.kkium.resume.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.INVALID_QUESTION_FOR_JD;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_NOT_FOUND;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.jd.domain.JdAnswer;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.jd.repository.JdAnswerRepository;
import com.kusitms.kkium.jd.repository.JdQuestionRepository;
import com.kusitms.kkium.resume.domain.AnswerExperience;
import com.kusitms.kkium.resume.dto.request.ResumeAnswerSaveRequest;
import com.kusitms.kkium.resume.repository.AnswerExperienceRepository;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResumeAnswerService {

  private final JdAnswerRepository jdAnswerRepository;
  private final JdQuestionRepository jdQuestionRepository;
  private final AnswerExperienceRepository answerExperienceRepository;
  private final UserRepository userRepository;

  @Transactional
  public void saveAnswers(Long jdId, Long userId, ResumeAnswerSaveRequest request) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new BaseException(USER_NOT_FOUND));

    // 1. 요청된 questionId 목록 추출
    List<Long> questionIds =
        request.answers().stream().map(ResumeAnswerSaveRequest.AnswerRequest::jdQuestionId).toList();

    // 2. jdId 검증 포함하여 한 번에 조회 (Security: 다른 공고 문항 접근 방지)
    List<JdQuestion> questions = jdQuestionRepository.findAllByIdInAndJdId(questionIds, jdId);
    if (questions.size() != questionIds.size()) {
      throw new BaseException(INVALID_QUESTION_FOR_JD);
    }
    Map<Long, JdQuestion> questionMap =
        questions.stream().collect(Collectors.toMap(JdQuestion::getId, Function.identity()));

    // 3. 기존 답변 한 번에 조회 후 Map으로 변환 (N+1 방지)
    List<JdAnswer> existingAnswers = jdAnswerRepository.findAllByJdQuestionInAndUser(questions, user);
    Map<Long, JdAnswer> answerMap =
        existingAnswers.stream()
            .collect(Collectors.toMap(a -> a.getJdQuestion().getId(), Function.identity()));

    // 4. upsert 처리
    List<JdAnswer> savedAnswers =
        request.answers().stream()
            .map(
                answerRequest -> {
                  JdQuestion question = questionMap.get(answerRequest.jdQuestionId());
                  String content = answerRequest.answerText() != null ? answerRequest.answerText() : "";

                  JdAnswer jdAnswer = answerMap.get(answerRequest.jdQuestionId());
                  if (jdAnswer == null) {
                    jdAnswer = jdAnswerRepository.save(
                        JdAnswer.builder().jdQuestion(question).user(user).content(content).build());
                  } else {
                    jdAnswer.updateContent(content);
                  }
                  return jdAnswer;
                })
            .toList();

    // 5. AnswerExperience 벌크 삭제 → insert (N+1 방지)
    List<Long> savedAnswerIds = savedAnswers.stream().map(JdAnswer::getId).toList();
    answerExperienceRepository.deleteAllByJdAnswerIdIn(savedAnswerIds);

    List<AnswerExperience> newExperiences =
        request.answers().stream()
            .filter(a -> a.experienceIds() != null)
            .flatMap(
                a -> {
                  JdAnswer jdAnswer = answerMap.getOrDefault(
                      a.jdQuestionId(),
                      savedAnswers.stream()
                          .filter(sa -> sa.getJdQuestion().getId().equals(a.jdQuestionId()))
                          .findFirst()
                          .orElseThrow());
                  return a.experienceIds().stream()
                      .map(expId -> AnswerExperience.builder().jdAnswer(jdAnswer).experienceId(expId).build());
                })
            .toList();

    answerExperienceRepository.saveAll(newExperiences);
  }
}
