package com.kusitms.kkium.jd.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.FORBIDDEN;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.JD_NOT_FOUND;

import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
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

  private static final String CACHE_KEY_PREFIX = "jd-analysis:";
  private final JdRepository jdRepository;
  private final ExperienceRepository experienceRepository;
  private final LlmMatchScoreService llmMatchScoreService;
  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;

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

    // 5. Redis 캐시 조회
    String cacheKey = CACHE_KEY_PREFIX + experienceId + ":" + jdId;
    try {
      String cached = redisTemplate.opsForValue().get(cacheKey);
      if (cached != null) {
        log.info("[경험 상세 분석] 캐시 히트 - experienceId={}, jdId={}", experienceId, jdId);
        return objectMapper.readValue(cached, JdExperienceAnalysisResponse.class);
      }
    } catch (Exception e) {
      log.warn("[경험 상세 분석] 캐시 조회 실패, LLM 호출로 fallback: {}", e.getMessage());
    }

    // 6. LLM 호출 - 좋은 점 / 부족한 점 / 활용 가이드 / 하이라이팅 키워드
    LlmExperienceDetailResult result = llmMatchScoreService.analyzeExperienceDetail(jd, experience);
    log.info(
        "[경험 상세 분석] LLM 호출 - experienceId={} | keywords={}",
        experienceId,
        result.highlightKeywords());

    JdExperienceAnalysisResponse response =
        new JdExperienceAnalysisResponse(
            experienceId,
            new ExperienceAnalysis(
                result.strengths(),
                result.weaknesses(),
                result.usageGuide(),
                result.highlightKeywords()));

    // 7. Redis 캐시 저장 (TTL 없음 - 경험/JD 수정, 삭제 시 명시적으로 무효화)
    try {
      redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response));
    } catch (Exception e) {
      log.warn("[경험 상세 분석] 캐시 저장 실패: {}", e.getMessage());
    }

    return response;
  }

  // 경험 수정/삭제 시 캐시 무효화 - jd-analysis:{experienceId}:*
  public void evictCache(Long experienceId) {
    try {
      Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + experienceId + ":*");
      if (keys != null && !keys.isEmpty()) {
        redisTemplate.delete(keys);
        log.info("[경험 상세 분석] 캐시 무효화 - experienceId={}, 삭제된 키 수={}", experienceId, keys.size());
      }
    } catch (Exception e) {
      log.warn("[경험 상세 분석] 캐시 무효화 실패: {}", e.getMessage());
    }
  }

  // JD 삭제 시 캐시 무효화 - jd-analysis:*:{jdId}
  public void evictCacheByJdId(Long jdId) {
    try {
      Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*:" + jdId);
      if (keys != null && !keys.isEmpty()) {
        redisTemplate.delete(keys);
        log.info("[경험 상세 분석] JD 캐시 무효화 - jdId={}, 삭제된 키 수={}", jdId, keys.size());
      }
    } catch (Exception e) {
      log.warn("[경험 상세 분석] JD 캐시 무효화 실패: {}", e.getMessage());
    }
  }
}
