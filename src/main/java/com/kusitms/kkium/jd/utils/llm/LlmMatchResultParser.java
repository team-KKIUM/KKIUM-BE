package com.kusitms.kkium.jd.utils.llm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;
import com.kusitms.kkium.jd.utils.llm.result.HighlightKeyword;
import com.kusitms.kkium.jd.utils.llm.result.LlmExperienceDetailResult;
import com.kusitms.kkium.jd.utils.llm.result.LlmMatchResult;
import com.kusitms.kkium.jd.utils.llm.result.LlmQuestionMatchResult;
import com.kusitms.kkium.jd.utils.llm.result.LlmWritingGuideResult;

import lombok.extern.slf4j.Slf4j;

/** LLM 응답 JSON 파싱만 담당하는 파서 */
@Slf4j
@Component
public class LlmMatchResultParser {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final List<String> VALID_SOURCES =
      List.of(
          "mainResponsibilities",
          "requiredQualifications",
          "preferredQualifications",
          "hardSkill",
          "softSkill");

  /**
   * 활용 적합도 + 지원 적합도 파싱
   *
   * @throws BaseException LLM_RESPONSE_INVALID — JSON 파싱 실패
   */
  public LlmMatchResult parseCombinedResult(String response, List<Experience> experiences) {
    try {
      JsonNode parsed = OBJECT_MAPPER.readTree(response);
      Map<Long, Integer> usageScores = parseUsageScores(parsed, buildFallbackScores(experiences));
      return new LlmMatchResult(
          usageScores, Math.max(0, Math.min(100, parsed.path("applicationScore").asInt(0))));
    } catch (BaseException e) {
      throw e;
    } catch (Exception e) {
      log.warn("LLM 응답 파싱 실패: {}", e.getMessage());
      throw new BaseException(ErrorCode.LLM_RESPONSE_INVALID);
    }
  }

  /**
   * 문항별 활용 적합도 파싱
   *
   * @throws BaseException LLM_RESPONSE_INVALID — JSON 파싱 실패
   */
  public LlmQuestionMatchResult parseQuestionMatchResult(
      String response, List<Experience> experiences) {
    try {
      JsonNode parsed = OBJECT_MAPPER.readTree(response);
      return new LlmQuestionMatchResult(parseUsageScores(parsed, buildFallbackScores(experiences)));
    } catch (BaseException e) {
      throw e;
    } catch (Exception e) {
      log.warn("문항별 LLM 응답 파싱 실패: {}", e.getMessage());
      throw new BaseException(ErrorCode.LLM_RESPONSE_INVALID);
    }
  }

  /**
   * 자소서 작성 가이드 파싱
   *
   * @throws BaseException LLM_RESPONSE_INVALID — 필수 필드 누락 또는 JSON 파싱 실패
   */
  public LlmWritingGuideResult parseWritingGuideResult(String response) {
    try {
      JsonNode parsed = OBJECT_MAPPER.readTree(response);
      if (!parsed.has("connectionToJd") || !parsed.has("writingGuide")) {
        throw new BaseException(ErrorCode.LLM_RESPONSE_INVALID);
      }
      List<String> keywords = new ArrayList<>();
      for (JsonNode k : parsed.path("coreKeywords")) keywords.add(k.asText());
      return new LlmWritingGuideResult(
          keywords, parsed.path("connectionToJd").asText(), parsed.path("writingGuide").asText());
    } catch (BaseException e) {
      throw e;
    } catch (Exception e) {
      log.warn("작성 가이드 LLM 응답 파싱 실패: {}", e.getMessage());
      throw new BaseException(ErrorCode.LLM_RESPONSE_INVALID);
    }
  }

  /**
   * 경험 상세 분석 파싱
   *
   * @throws BaseException LLM_RESPONSE_INVALID — 필수 필드 누락 또는 JSON 파싱 실패
   */
  public LlmExperienceDetailResult parseExperienceDetailResult(String response) {
    try {
      JsonNode parsed = OBJECT_MAPPER.readTree(response);
      if (!parsed.has("strengths") || !parsed.has("weaknesses") || !parsed.has("usageGuide")) {
        throw new BaseException(ErrorCode.LLM_RESPONSE_INVALID);
      }
      List<HighlightKeyword> keywords = new ArrayList<>();
      for (JsonNode k : parsed.path("highlightKeywords")) {
        String keyword = k.path("keyword").asText();
        List<String> sources = new ArrayList<>();
        for (JsonNode s : k.path("sources")) {
          String source = s.asText();
          if (VALID_SOURCES.contains(source)) sources.add(source);
        }
        keywords.add(new HighlightKeyword(keyword, sources));
      }
      return new LlmExperienceDetailResult(
          parsed.path("strengths").asText(),
          parsed.path("weaknesses").asText(),
          parsed.path("usageGuide").asText(),
          keywords);
    } catch (BaseException e) {
      throw e;
    } catch (Exception e) {
      log.warn("경험 상세 분석 LLM 응답 파싱 실패: {}", e.getMessage());
      throw new BaseException(ErrorCode.LLM_RESPONSE_INVALID);
    }
  }

  // 공통 유틸

  private Map<Long, Integer> buildFallbackScores(List<Experience> experiences) {
    return experiences.stream()
        .collect(
            Collectors.toMap(
                e -> e.getPiece().getId(), e -> 50, (existing, replacement) -> existing));
  }

  private Map<Long, Integer> parseUsageScores(JsonNode parsed, Map<Long, Integer> fallback) {
    Map<Long, Integer> usageScores = new HashMap<>();
    for (JsonNode item : parsed.path("usageScores")) {
      usageScores.put(
          item.path("pieceId").asLong(), Math.max(0, Math.min(100, item.path("score").asInt(50))));
    }
    fallback.forEach(usageScores::putIfAbsent);
    return usageScores;
  }
}
