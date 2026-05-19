package com.kusitms.kkium.jd.utils.llm;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.jd.domain.Jd;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LlmMatchScoreService {

  private static final String GEMINI_URL =
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final WebClient webClient;
  private final String apiKey;

  public LlmMatchScoreService(WebClient webClient, @Value("${gemini.api-key}") String apiKey) {
    this.webClient = webClient;
    this.apiKey = apiKey;
  }

  /** LLM 1번 호출로 활용 적합도(경험별) + 지원 적합도(포트폴리오 종합)를 한꺼번에 반환 */
  public LlmMatchResult scoreAll(Jd jd, List<Experience> experiences) {
    if (experiences.isEmpty()) {
      return new LlmMatchResult(Map.of(), 0);
    }

    String prompt = buildCombinedPrompt(jd, experiences);
    String response = callGemini(prompt);
    return parseCombinedResult(response, experiences);
  }

  public record LlmMatchResult(
      Map<Long, Integer> usageScores, // pieceId → 활용 적합도 LLM 점수
      int applicationScore // 지원 적합도 LLM 점수
      ) {}

  private String buildCombinedPrompt(Jd jd, List<Experience> experiences) {
    String expList =
        IntStream.range(0, experiences.size())
            .mapToObj(
                i -> {
                  Experience e = experiences.get(i);
                  return """
              경험 %d (pieceId: %d):
              - 제목: %s
              - 한줄소개: %s
              - Situation: %s
              - Task: %s
              - Action: %s
              - Result: %s
              - Taken: %s
              """
                      .formatted(
                          i + 1,
                          e.getPiece().getId(),
                          e.getTitle(),
                          e.getOneLineIntro(),
                          e.getSituation(),
                          e.getTask(),
                          e.getAct(),
                          e.getResult(),
                          e.getTaken());
                })
            .collect(Collectors.joining("\n"));

    return """
        너는 채용 공고와 경험의 적합도를 평가하는 시스템이다.

        [규칙]
        - 반드시 제공된 내용만 근거로 사용하라.
        - 없는 내용을 추론하지 마라.
        - 점수는 0~100 사이 정수로 반환하라.
        - 반드시 JSON 형식으로만 응답하라.

        [공고 정보]
        - 기업: %s
        - 직무: %s
        - 주요 업무: %s
        - 필수 역량: %s
        - 기술 스택: %s
        - 소프트 스킬: %s

        [경험 목록]
        %s

        [해야 할 일]
        1. 각 경험이 위 공고에 개별적으로 얼마나 활용 가능한지 평가하라. (usageScores)
        2. 위 경험들을 포트폴리오 전체 관점에서 공고 요구사항을 얼마나 커버하는지 평가하라. (applicationScore)

        반환 형식:
        {
          "usageScores": [
            { "pieceId": 숫자, "score": 숫자 },
            ...
          ],
          "applicationScore": 숫자
        }
        """
        .formatted(
            jd.getCompanyName(),
            jd.getRecruitmentField(),
            jd.getMainResponsibilities(),
            jd.getRequiredQualifications(),
            jd.getHardSkill(),
            jd.getSoftSkill(),
            expList);
  }

  private String callGemini(String prompt) {
    Map<String, Object> body =
        Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
            "generationConfig", Map.of("responseMimeType", "application/json"));

    try {
      return webClient
          .post()
          .uri(GEMINI_URL + "?key=" + apiKey)
          .header("Content-Type", "application/json")
          .bodyValue(body)
          .retrieve()
          .bodyToMono(String.class)
          .block();
    } catch (Exception e) {
      log.warn("Gemini 호출 실패: {}", e.getMessage());
      return null;
    }
  }

  private LlmMatchResult parseCombinedResult(String response, List<Experience> experiences) {
    // 기본값: 모든 경험 50점, 지원 적합도 0점
    Map<Long, Integer> fallbackScores =
        experiences.stream().collect(Collectors.toMap(e -> e.getPiece().getId(), e -> 50));

    if (response == null) return new LlmMatchResult(fallbackScores, 0);

    try {
      JsonNode root = OBJECT_MAPPER.readTree(response);
      String json = extractJsonText(root);
      JsonNode parsed = OBJECT_MAPPER.readTree(json);

      // 활용 적합도 파싱
      Map<Long, Integer> usageScores = new java.util.HashMap<>();
      for (JsonNode item : parsed.path("usageScores")) {
        long pieceId = item.path("pieceId").asLong();
        int score = Math.max(0, Math.min(100, item.path("score").asInt(50)));
        usageScores.put(pieceId, score);
      }
      fallbackScores.forEach(usageScores::putIfAbsent);

      // 지원 적합도 파싱
      int applicationScore = Math.max(0, Math.min(100, parsed.path("applicationScore").asInt(0)));

      return new LlmMatchResult(usageScores, applicationScore);
    } catch (Exception e) {
      log.warn("LLM 응답 파싱 실패: {}", e.getMessage());
      return new LlmMatchResult(fallbackScores, 0);
    }
  }

  private String extractJsonText(JsonNode root) {
    JsonNode textNode =
        root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
    if (!textNode.isMissingNode() && !textNode.asText().isBlank()) {
      return textNode.asText();
    }
    return root.toString();
  }
}
