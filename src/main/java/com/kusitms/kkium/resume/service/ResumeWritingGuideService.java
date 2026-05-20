package com.kusitms.kkium.resume.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_SELECTION_LIMIT;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.FORBIDDEN;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.JD_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.QUESTION_NOT_FOUND;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.repository.ExperienceRepository;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.jd.repository.JdQuestionRepository;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.jd.utils.llm.LlmMatchScoreService;
import com.kusitms.kkium.jd.utils.llm.LlmMatchScoreService.LlmWritingGuideResult;
import com.kusitms.kkium.resume.dto.response.ResumeWritingGuideResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeWritingGuideService {

  private final JdRepository jdRepository;
  private final JdQuestionRepository jdQuestionRepository;
  private final ExperienceRepository experienceRepository;
  private final LlmMatchScoreService llmMatchScoreService;

  public ResumeWritingGuideResponse generateGuide(
      Long jdId, Long questionId, List<Long> experienceIds, Long userId) {

    // 1. 경험 개수 검증 (1~3개)
    if (experienceIds == null || experienceIds.isEmpty() || experienceIds.size() > 3) {
      throw new BaseException(EXPERIENCE_SELECTION_LIMIT);
    }

    // 2. JD 조회 + 소유자 검증
    Jd jd = jdRepository.findById(jdId).orElseThrow(() -> new BaseException(JD_NOT_FOUND));
    if (!jd.getUser().getId().equals(userId)) {
      throw new BaseException(FORBIDDEN);
    }

    // 3. 문항 조회
    JdQuestion question =
        jdQuestionRepository
            .findById(questionId)
            .orElseThrow(() -> new BaseException(QUESTION_NOT_FOUND));

    // 4. 경험 목록 조회 + 소유자 검증
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

    // 5. LLM 호출 — 작성 가이드 생성
    LlmWritingGuideResult result =
        llmMatchScoreService.generateWritingGuide(jd, question, experiences);

    log.info(
        "[작성 가이드] jdId={} | questionId={} | experienceIds={} | keywords={}",
        jdId,
        questionId,
        experienceIds,
        result.coreKeywords());

    return new ResumeWritingGuideResponse(
        result.coreKeywords(), result.connectionToJd(), result.writingGuide());
  }
}
