package com.kusitms.kkium.jd.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.INVALID_INPUT_VALUE;
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
import com.kusitms.kkium.jd.dto.response.JdWritingGuideResponse;
import com.kusitms.kkium.jd.repository.JdQuestionRepository;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.jd.utils.llm.LlmMatchScoreService;
import com.kusitms.kkium.jd.utils.llm.LlmMatchScoreService.LlmWritingGuideResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JdWritingGuideService {

  private final JdRepository jdRepository;
  private final JdQuestionRepository jdQuestionRepository;
  private final ExperienceRepository experienceRepository;
  private final LlmMatchScoreService llmMatchScoreService;

  public JdWritingGuideResponse generateGuide(
      Long jdId, Long questionId, List<Long> experienceIds) {

    // 1. 경험 개수 검증 (1~3개)
    if (experienceIds == null || experienceIds.isEmpty() || experienceIds.size() > 3) {
      throw new BaseException(INVALID_INPUT_VALUE);
    }

    // 2. JD 조회
    Jd jd = jdRepository.findById(jdId).orElseThrow(() -> new BaseException(JD_NOT_FOUND));

    // 3. 문항 조회
    JdQuestion question =
        jdQuestionRepository
            .findById(questionId)
            .orElseThrow(() -> new BaseException(QUESTION_NOT_FOUND));

    // 4. 경험 목록 조회 (순서 보장: experienceIds 순서대로)
    List<Experience> experiences =
        experienceIds.stream()
            .map(
                id ->
                    experienceRepository
                        .findByIdWithPiece(id)
                        .orElseThrow(() -> new BaseException(EXPERIENCE_NOT_FOUND)))
            .toList();

    // 5. LLM 호출 — 작성 가이드 생성
    LlmWritingGuideResult result =
        llmMatchScoreService.generateWritingGuide(jd, question, experiences);

    log.info(
        "[작성 가이드] jdId={} | questionId={} | experienceIds={} | keywords={}",
        jdId,
        questionId,
        experienceIds,
        result.coreKeywords());

    return new JdWritingGuideResponse(
        result.coreKeywords(), result.connectionToJd(), result.writingGuide());
  }
}
