package com.kusitms.kkium.resume.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.AI_DRAFT_ALREADY_EXISTS;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_SELECTION_LIMIT;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.FORBIDDEN;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.JD_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.QUESTION_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_NOT_FOUND;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.repository.ExperienceRepository;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdAnswer;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.jd.repository.JdAnswerRepository;
import com.kusitms.kkium.jd.repository.JdQuestionRepository;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.resume.dto.response.AiDraftResponse;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAiDraftService {

  private final JdRepository jdRepository;
  private final JdQuestionRepository jdQuestionRepository;
  private final JdAnswerRepository jdAnswerRepository;
  private final ExperienceRepository experienceRepository;
  private final UserRepository userRepository;
  private final GeminiAiDraftService geminiAiDraftService;

  @Transactional
  public AiDraftResponse generateAiDraft(
      Long jdId, Long questionId, List<Long> experienceIds, Long userId) {

    if (experienceIds == null || experienceIds.isEmpty() || experienceIds.size() > 3) {
      throw new BaseException(EXPERIENCE_SELECTION_LIMIT);
    }

    Jd jd = jdRepository.findById(jdId).orElseThrow(() -> new BaseException(JD_NOT_FOUND));
    if (!jd.getUser().getId().equals(userId)) {
      throw new BaseException(FORBIDDEN);
    }

    JdQuestion question =
        jdQuestionRepository
            .findById(questionId)
            .orElseThrow(() -> new BaseException(QUESTION_NOT_FOUND));

    List<Experience> experiences = experienceRepository.findAllByIdIn(experienceIds);
    if (experiences.size() != experienceIds.size()) {
      throw new BaseException(EXPERIENCE_NOT_FOUND);
    }
    experiences.forEach(
        exp -> {
          if (!exp.getPiece().getUser().getId().equals(userId)) {
            throw new BaseException(FORBIDDEN);
          }
        });

    User user =
        userRepository.findById(userId).orElseThrow(() -> new BaseException(USER_NOT_FOUND));

    jdAnswerRepository
        .findByJdQuestionAndUser(question, user)
        .ifPresent(
            existing -> {
              if (existing.getAiDraft() != null) {
                throw new BaseException(AI_DRAFT_ALREADY_EXISTS);
              }
            });

    String draft = geminiAiDraftService.generateAiDraft(jd, question, experiences);

    JdAnswer answer =
        jdAnswerRepository
            .findByJdQuestionAndUser(question, user)
            .orElseGet(
                () ->
                    jdAnswerRepository.save(
                        JdAnswer.builder().jdQuestion(question).user(user).content("").build()));
    answer.updateAiDraft(draft);

    log.info("[AI 초안] jdId={} | questionId={} | experienceIds={}", jdId, questionId, experienceIds);

    return new AiDraftResponse(draft);
  }
}
