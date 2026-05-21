package com.kusitms.kkium.resume.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.QUESTION_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_NOT_FOUND;

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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResumeAnswerService {

  private final JdAnswerRepository jdAnswerRepository;
  private final JdQuestionRepository jdQuestionRepository;
  private final AnswerExperienceRepository answerExperienceRepository;
  private final UserRepository userRepository;

  @Transactional
  public void saveAnswers(Long jdId, Long userId, ResumeAnswerSaveRequest request) {
    User user = userRepository.findById(userId).orElseThrow(() -> new BaseException(USER_NOT_FOUND));

    for (ResumeAnswerSaveRequest.AnswerRequest answerRequest : request.answers()) {
      JdQuestion question =
          jdQuestionRepository
              .findById(answerRequest.jdQuestionId())
              .orElseThrow(() -> new BaseException(QUESTION_NOT_FOUND));

      // upsert: 기존 답변 있으면 update, 없으면 insert
      JdAnswer jdAnswer =
          jdAnswerRepository
              .findByJdQuestionAndUser(question, user)
              .orElseGet(
                  () ->
                      jdAnswerRepository.save(
                          JdAnswer.builder()
                              .jdQuestion(question)
                              .user(user)
                              .content(answerRequest.answerText() != null ? answerRequest.answerText() : "")
                              .build()));

      jdAnswer.updateContent(answerRequest.answerText() != null ? answerRequest.answerText() : "");

      // AnswerExperience delete → insert
      answerExperienceRepository.deleteByJdAnswerId(jdAnswer.getId());

      if (answerRequest.experienceIds() != null) {
        List<AnswerExperience> experiences =
            answerRequest.experienceIds().stream()
                .map(expId -> AnswerExperience.builder().jdAnswer(jdAnswer).experienceId(expId).build())
                .toList();
        answerExperienceRepository.saveAll(experiences);
      }
    }
  }
}
