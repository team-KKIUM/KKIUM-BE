package com.kusitms.kkium.jd.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.FORBIDDEN;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.JD_NOT_FOUND;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.repository.ExperienceRepository;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.dto.response.JdExperienceAnalysisResponse;
import com.kusitms.kkium.jd.dto.response.JdExperienceAnalysisResponse.ExperienceAnalysis;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.jd.utils.llm.LlmMatchScoreService;
import com.kusitms.kkium.jd.utils.llm.LlmMatchScoreService.LlmExperienceDetailResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JdExperienceAnalysisService {

  private final JdRepository jdRepository;
  private final ExperienceRepository experienceRepository;
  private final LlmMatchScoreService llmMatchScoreService;

  public JdExperienceAnalysisResponse analyze(Long jdId, Long experienceId, Long userId) {
    // 1. JD 조회
    Jd jd = jdRepository.findById(jdId).orElseThrow(() -> new BaseException(JD_NOT_FOUND));

    // 2. 소유자 검증
    if (!jd.getUser().getId().equals(userId)) {
      throw new BaseException(FORBIDDEN);
    }

    // 3. 경험 조회
    Experience experience =
        experienceRepository
            .findByIdWithPiece(experienceId)
            .orElseThrow(() -> new BaseException(EXPERIENCE_NOT_FOUND));

    // 4. 경험 소유자 검증
    if (!experience.getPiece().getUser().getId().equals(userId)) {
      throw new BaseException(FORBIDDEN);
    }

    // 4. LLM 호출 - 좋은 점 / 부족한 점 / 활용 가이드 / 하이라이팅 키워드
    LlmExperienceDetailResult result = llmMatchScoreService.analyzeExperienceDetail(jd, experience);

    log.info("[경험 상세 분석] experienceId={} | keywords={}", experienceId, result.highlightKeywords());

    return new JdExperienceAnalysisResponse(
        experienceId,
        new ExperienceAnalysis(
            result.strengths(),
            result.weaknesses(),
            result.usageGuide(),
            result.highlightKeywords()));
  }
}
