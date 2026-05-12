package com.kusitms.kkium.jd.utils;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LlmJdAnalyzer {

  private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
  private static final int MAX_INPUT_LENGTH = 20_000;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final WebClient webClient;
  private final String apiKey;

  public LlmJdAnalyzer(WebClient webClient, @Value("${openai.api.key}") String apiKey) {
    this.webClient = webClient;
    this.apiKey = apiKey;
  }

  public AnalyzedJd analyze(String content) {
    if (content == null || content.isBlank()) return AnalyzedJd.empty();

    String input =
        content.length() > MAX_INPUT_LENGTH ? content.substring(0, MAX_INPUT_LENGTH) : content;

    String systemPrompt =
        """
        너는 채용공고 텍스트를 분석해 지원자에게 유용한 정보를 구조화하는 전문가야.
        주어진 공고 본문에서 아래 JSON 형식으로 정보를 추출해줘.

        중요: 텍스트에 명확히 존재하는 정보만 추출해. 추측하거나 만들어내지 마.
        정보를 찾을 수 없으면 반드시 null을 반환해.

        hardSkill 추출 규칙:
        - 기술 스택, 프레임워크, 언어, 도구 등 기술적 역량을 쉼표로 구분해 추출해. 예: "Java, Spring Boot, AWS, Docker, PostgreSQL"
        - 텍스트에 기술 스택이 없으면 null을 반환해.

        softSkill 추출 규칙:
        - 소통, 협업, 문제해결 등 직무 수행에 필요한 소프트 스킬을 쉼표로 구분해 추출해. 예: "커뮤니케이션, 문제해결, 팀워크"
        - 텍스트에 소프트 스킬이 없으면 null을 반환해.

        mainResponsibilities 추출 규칙:
        - 주요 업무 내용을 원문 그대로 추출해.
        - 없으면 null을 반환해.

        requiredQualifications 추출 규칙:
        - 필수 자격 요건 / 필수 역량을 원문 그대로 추출해.
        - 없으면 null을 반환해.

        preferredQualifications 추출 규칙:
        - 우대 사항 / 우대 역량을 원문 그대로 추출해.
        - 없으면 null을 반환해.

        {
          "hardSkill": "기술 태그 (쉼표 구분)",
          "softSkill": "소프트 스킬 태그 (쉼표 구분)",
          "mainResponsibilities": "주요 업무 내용",
          "requiredQualifications": "필수 역량/자격",
          "preferredQualifications": "우대 역량/자격"
        }
        """;

    Map<String, Object> body =
        Map.of(
            "model", "gpt-4o-mini",
            "response_format", Map.of("type", "json_object"),
            "messages",
                List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", input)));

    try {
      String response =
          webClient
              .post()
              .uri(OPENAI_URL)
              .header("Authorization", "Bearer " + apiKey)
              .header("Content-Type", "application/json")
              .bodyValue(body)
              .retrieve()
              .bodyToMono(String.class)
              .block();

      return extractFromResponse(response);
    } catch (Exception e) {
      log.warn("JD 분석 요청 실패: {}", e.getMessage());
      return AnalyzedJd.empty();
    }
  }

  private AnalyzedJd extractFromResponse(String response) {
    try {
      JsonNode root = OBJECT_MAPPER.readTree(response);
      String content = root.path("choices").get(0).path("message").path("content").asText();
      JsonNode parsed = OBJECT_MAPPER.readTree(content);

      return new AnalyzedJd(
          parsed.path("hardSkill").asText(null),
          parsed.path("softSkill").asText(null),
          parsed.path("mainResponsibilities").asText(null),
          parsed.path("requiredQualifications").asText(null),
          parsed.path("preferredQualifications").asText(null));
    } catch (Exception e) {
      log.warn("JD 분석 응답 파싱 실패: {}", e.getMessage());
      return AnalyzedJd.empty();
    }
  }

  public record AnalyzedJd(
      String hardSkill,
      String softSkill,
      String mainResponsibilities,
      String requiredQualifications,
      String preferredQualifications) {

    static AnalyzedJd empty() {
      return new AnalyzedJd(null, null, null, null, null);
    }
  }
}
