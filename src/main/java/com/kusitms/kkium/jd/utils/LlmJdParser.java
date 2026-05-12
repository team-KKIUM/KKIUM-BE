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
public class LlmJdParser {

  private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
  private static final int MAX_INPUT_LENGTH = 20_000;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final WebClient webClient;
  private final String apiKey;

  public LlmJdParser(WebClient webClient, @Value("${openai.api.key}") String apiKey) {
    this.webClient = webClient;
    this.apiKey = apiKey;
  }

  public ParsedJd parse(String rawText) {
    String input =
        rawText.length() > MAX_INPUT_LENGTH ? rawText.substring(0, MAX_INPUT_LENGTH) : rawText;

    String systemPrompt =
        """
        너는 채용공고 텍스트를 분석하는 전문가야.
        주어진 텍스트에서 아래 JSON 형식으로 정보를 추출해줘.
        날짜는 "yyyy-MM-dd" 형식으로 반환해줘. 알 수 없으면 null로 반환해.

        중요: 텍스트에 명확히 존재하는 정보만 추출해. 추측하거나 만들어내지 마.
        정보를 찾을 수 없으면 반드시 null을 반환해.

        companyName 추출 규칙:
        - (주), (유), (사), 주식회사, 유한회사 등 법인 형태 표기는 제거해. 예: "(주)카카오" → "카카오"

        recruitmentField 추출 규칙:
        - 실제 모집 직무명을 추출해. 예: "AI 서비스 기획", "백엔드 개발", "IDC 운영자"
        - 카테고리 태그(예: "IT/인터넷", "경영/사무", "생산/제조")는 절대 사용하지 마.
        - 고용형태(예: "신입", "경력", "인턴")는 절대 사용하지 마.
        - 직무가 여러 개 나열된 경우 텍스트에 명시된 직무명을 모두 쉼표로 구분해서 반환해. 예: "생산기술, 설비기술, AI, 마케팅"
        - 직무명이 명확하지 않으면 공고 제목에서 직무명 부분만 뽑아내면 돼.

        questions 추출 규칙:
        - 지원자가 직접 작성해야 하는 자기소개서 문항만 추출해.
        - 텍스트에 문항이 없으면 빈 배열 []을 반환해. 절대 만들어내지 마.

        content 추출 규칙:
        - 해당 채용공고의 본문 내용만 추출해. (모집 분야, 지원 자격, 우대 사항, 근무 조건 등)
        - 네비게이션 메뉴, 광고, 다른 공고 목록, 채팅 내용, 푸터 등 공고와 무관한 내용은 제외해.
        - 원문 텍스트를 그대로 유지하되 노이즈만 걷어내. 요약하거나 변형하지 마.
        - 본문을 찾을 수 없으면 null을 반환해.

        {
          "title": "공고 제목",
          "companyName": "기업명",
          "recruitmentField": "모집 직무명 (카테고리/고용형태 제외)",
          "startDate": "yyyy-MM-dd",
          "endDate": "yyyy-MM-dd",
          "questions": ["자소서 문항1", "자소서 문항2"],
          "content": "공고 본문 내용"
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
      log.warn("LLM 파싱 요청 실패: {}", e.getMessage());
      return ParsedJd.empty();
    }
  }

  private ParsedJd extractFromResponse(String response) {
    try {
      JsonNode root = OBJECT_MAPPER.readTree(response);
      String content = root.path("choices").get(0).path("message").path("content").asText();
      JsonNode parsed = OBJECT_MAPPER.readTree(content);

      List<String> questions = List.of();
      if (parsed.has("questions") && parsed.get("questions").isArray()) {
        questions =
            OBJECT_MAPPER.convertValue(
                parsed.get("questions"),
                OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, String.class));
      }

      return new ParsedJd(
          parsed.path("title").asText(null),
          parsed.path("companyName").asText(null),
          parsed.path("recruitmentField").asText(null),
          parsed.path("startDate").asText(null),
          parsed.path("endDate").asText(null),
          questions,
          parsed.path("content").asText(null));
    } catch (Exception e) {
      log.warn("LLM 응답 파싱 실패: {}", e.getMessage());
      return ParsedJd.empty();
    }
  }

  public record ParsedJd(
      String title,
      String companyName,
      String recruitmentField,
      String startDate,
      String endDate,
      List<String> questions,
      String content) {

    static ParsedJd empty() {
      return new ParsedJd(null, null, null, null, null, List.of(), null);
    }
  }
}
