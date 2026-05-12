package com.kusitms.kkium.experience.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kusitms.kkium.experience.dto.response.ExperienceAnalyzeResponse;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiLlmService implements LlmService {

  private static final String GEMINI_API_URL =
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

  private final WebClient webClient;
  private final ObjectMapper objectMapper;

  @Value("${gemini.api-key}")
  private String apiKey;

  @Override
  public ExperienceAnalyzeResponse analyze(Long userId, String extractedText) {
    String prompt = buildPrompt(extractedText);
    String rawJson = callGeminiApi(prompt);
    return parseResponse(rawJson);
  }

  private String buildPrompt(String extractedText) {
    return """
        아래 경험 자료를 분석하여 JSON 형식으로만 응답해주세요.
        모든 응답은 반드시 한국어로 작성해주세요.
        JSON 외 다른 텍스트는 절대 포함하지 마세요.

        경험 자료:
        %s

        응답 형식:
        {
          "title": "경험 제목 (간결하게)",
          "oneLineIntro": "한 줄 소개",
          "activityInfo": {
            "name": "활동명 (추론 가능하면 채우고, 아니면 null)",
            "teamNum": 팀원수 또는 null,
            "startDate": "YYYY-MM-DD 또는 null",
            "endDate": "YYYY-MM-DD 또는 null",
            "contributionRate": 기여도(0-100) 또는 null,
            "role": "역할 또는 null"
          },
          "careerInfo": {
            "name": "직무명 (추론 가능하면 채우고, 아니면 null)",
            "company": "회사명 또는 null",
            "employmentStatus": "고용형태 또는 null",
            "startDate": "YYYY-MM-DD 또는 null",
            "endDate": "YYYY-MM-DD 또는 null"
          },
          "educationInfo": {
            "organizationName": "교육기관명 (추론 가능하면 채우고, 아니면 null)",
            "name": "수강명 또는 null",
            "startDate": "YYYY-MM-DD 또는 null",
            "endDate": "YYYY-MM-DD 또는 null"
          },
          "situation": "상황 및 목표",
          "task": "해결 과제",
          "act": "실제 행동",
          "result": "결과 및 성과",
          "taken": "배운 점"
        }
        """
        .formatted(extractedText);
  }

  private String callGeminiApi(String prompt) {
    Map<String, Object> requestBody =
        Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));

    try {
      return webClient
          .post()
          .uri(GEMINI_API_URL + "?key=" + apiKey)
          .bodyValue(requestBody)
          .retrieve()
          .bodyToMono(String.class)
          .block();
    } catch (Exception e) {
      log.error("Gemini API 호출 실패: {}", e.getMessage());
      throw new BaseException(ErrorCode.LLM_CALL_FAILED);
    }
  }

  private ExperienceAnalyzeResponse parseResponse(String rawResponse) {
    try {
      JsonNode root = objectMapper.readTree(rawResponse);
      String jsonText =
          root.path("candidates")
              .get(0)
              .path("content")
              .path("parts")
              .get(0)
              .path("text")
              .asText();

      // 코드블록 제거 (```json ... ``` 형태로 올 수 있음)
      jsonText = jsonText.replaceAll("```json", "").replaceAll("```", "").trim();

      return objectMapper.readValue(jsonText, ExperienceAnalyzeResponse.class);
    } catch (JsonProcessingException e) {
      log.error("Gemini 응답 파싱 실패: {}", e.getMessage());
      throw new BaseException(ErrorCode.LLM_CALL_FAILED);
    }
  }
}
