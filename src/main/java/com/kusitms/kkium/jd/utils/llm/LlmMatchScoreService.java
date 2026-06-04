package com.kusitms.kkium.jd.utils.llm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.jd.utils.llm.result.*;
import com.kusitms.kkium.jd.utils.llm.result.HighlightKeyword;
import com.kusitms.kkium.jd.utils.llm.result.LlmExperienceDetailResult;
import com.kusitms.kkium.jd.utils.llm.result.LlmMatchResult;
import com.kusitms.kkium.jd.utils.llm.result.LlmQuestionMatchResult;
import com.kusitms.kkium.jd.utils.llm.result.LlmWritingGuideResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LlmMatchScoreService {

  private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
  private static final String MODEL = "gpt-4o";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final WebClient webClient;
  private final String apiKey;
  private final JdMatchPromptBuilder promptBuilder;

  public LlmMatchScoreService(
      WebClient webClient,
      @Value("${openai.api.key}") String apiKey,
      JdMatchPromptBuilder promptBuilder) {
    this.webClient = webClient;
    this.apiKey = apiKey;
    this.promptBuilder = promptBuilder;
  }

  /** LLM 1번 호출로 활용 적합도(경험별) + 지원 적합도(포트폴리오 종합)를 한꺼번에 반환 */
  public LlmMatchResult scoreAll(Jd jd, List<Experience> experiences) {
    if (experiences.isEmpty()) return new LlmMatchResult(Map.of(), 0);
    String prompt = promptBuilder.buildCombinedPrompt(jd, experiences);
    String response = callOpenAi(prompt, promptBuilder.buildCombinedSchema());
    return parseCombinedResult(response, experiences);
  }

  /** 문항 컨텍스트를 포함해 활용 적합도를 계산 (자소서 작성 화면 경험 선택 모달용) */
  public LlmQuestionMatchResult scoreAllByQuestion(
      Jd jd, JdQuestion question, List<Experience> experiences) {
    if (experiences.isEmpty()) return new LlmQuestionMatchResult(Map.of());
    String prompt = promptBuilder.buildQuestionCombinedPrompt(jd, question, experiences);
    String response = callOpenAi(prompt, promptBuilder.buildQuestionCombinedSchema());
    return parseQuestionMatchResult(response, experiences);
  }

  /** 선택된 경험들을 기반으로 자소서 작성 가이드 생성 */
  public LlmWritingGuideResult generateWritingGuide(
      Jd jd, JdQuestion question, List<Experience> experiences) {
    if (experiences.isEmpty()) return LlmWritingGuideResult.empty();
    String prompt = promptBuilder.buildWritingGuidePrompt(jd, question, experiences);
    String response = callOpenAi(prompt, promptBuilder.buildWritingGuideSchema());
    return parseWritingGuideResult(response);
  }

  /** 경험 카드 클릭 시 상세 분석 */
  public LlmExperienceDetailResult analyzeExperienceDetail(Jd jd, Experience experience) {
    String prompt = promptBuilder.buildExperienceDetailPrompt(jd, experience);
    String response = callOpenAi(prompt, promptBuilder.buildExperienceDetailSchema());
    return parseExperienceDetailResult(response);
  }

  // API 호출
  private String callOpenAi(String prompt, Map<String, Object> jsonSchema) {
    Map<String, Object> body = new HashMap<>();
    body.put("model", MODEL);
    body.put("temperature", 0);
    body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
    body.put("response_format", Map.of("type", "json_schema", "json_schema", jsonSchema));
    try {
      String response =
          webClient
              .post()
              .uri(OPENAI_URL)
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + apiKey)
              .bodyValue(body)
              .retrieve()
              .bodyToMono(String.class)
              .block();

      JsonNode root = OBJECT_MAPPER.readTree(response);
      return root.path("choices").path(0).path("message").path("content").asText();
    } catch (Exception e) {
      log.warn("OpenAI 호출 실패: {}", e.getMessage());
      return null;
    }
  }

  // 응답 파싱

  private LlmMatchResult parseCombinedResult(String response, List<Experience> experiences) {
    Map<Long, Integer> fallback = buildFallbackScores(experiences);
    if (response == null) return new LlmMatchResult(fallback, 0);
    try {
      JsonNode parsed = OBJECT_MAPPER.readTree(response);
      Map<Long, Integer> usageScores = parseUsageScores(parsed, fallback);
      return new LlmMatchResult(
          usageScores, Math.max(0, Math.min(100, parsed.path("applicationScore").asInt(0))));
    } catch (Exception e) {
      log.warn("LLM 응답 파싱 실패: {}", e.getMessage());
      return new LlmMatchResult(fallback, 0);
    }
  }

  private LlmQuestionMatchResult parseQuestionMatchResult(
      String response, List<Experience> experiences) {
    Map<Long, Integer> fallback = buildFallbackScores(experiences);
    if (response == null) return new LlmQuestionMatchResult(fallback);
    try {
      JsonNode parsed = OBJECT_MAPPER.readTree(response);
      return new LlmQuestionMatchResult(parseUsageScores(parsed, fallback));
    } catch (Exception e) {
      log.warn("문항별 LLM 응답 파싱 실패: {}", e.getMessage());
      return new LlmQuestionMatchResult(fallback);
    }
  }

  private LlmWritingGuideResult parseWritingGuideResult(String response) {
    if (response == null) return LlmWritingGuideResult.empty();
    try {
      JsonNode parsed = OBJECT_MAPPER.readTree(response);
      List<String> keywords = new ArrayList<>();
      for (JsonNode k : parsed.path("coreKeywords")) keywords.add(k.asText());
      return new LlmWritingGuideResult(
          keywords,
          parsed.path("connectionToJd").asText("분석에 실패했습니다."),
          parsed.path("writingGuide").asText("분석에 실패했습니다."));
    } catch (Exception e) {
      log.warn("작성 가이드 LLM 응답 파싱 실패: {}", e.getMessage());
      return LlmWritingGuideResult.empty();
    }
  }

  private static final List<String> VALID_SOURCES =
      List.of(
          "mainResponsibilities",
          "requiredQualifications",
          "preferredQualifications",
          "hardSkill",
          "softSkill");

  private LlmExperienceDetailResult parseExperienceDetailResult(String response) {
    if (response == null) return LlmExperienceDetailResult.empty();
    try {
      JsonNode parsed = OBJECT_MAPPER.readTree(response);
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
          parsed.path("strengths").asText("분석에 실패했습니다."),
          parsed.path("weaknesses").asText("분석에 실패했습니다."),
          parsed.path("usageGuide").asText("분석에 실패했습니다."),
          keywords);
    } catch (Exception e) {
      log.warn("경험 상세 분석 LLM 응답 파싱 실패: {}", e.getMessage());
      return LlmExperienceDetailResult.empty();
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
