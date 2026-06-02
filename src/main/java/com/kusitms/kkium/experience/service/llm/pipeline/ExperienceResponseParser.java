package com.kusitms.kkium.experience.service.llm.pipeline;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kusitms.kkium.experience.dto.response.ExperienceAnalyzeResponse;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExperienceResponseParser {

  private final ObjectMapper objectMapper;

  public ExperienceAnalyzeResponse parse(String rawResponse) {
    if (rawResponse == null || rawResponse.isBlank()) {
      log.error("Gemini 응답이 비어있음");
      throw new BaseException(ErrorCode.LLM_RESPONSE_INVALID);
    }

    String jsonText = extractJsonText(rawResponse);

    try {
      return objectMapper.readValue(jsonText, ExperienceAnalyzeResponse.class);
    } catch (JsonProcessingException e) {
      log.error("Gemini 응답 스키마 위반: {}", e.getMessage());
      throw new BaseException(ErrorCode.LLM_SCHEMA_VIOLATION);
    }
  }

  private String extractJsonText(String rawResponse) {
    try {
      JsonNode root = objectMapper.readTree(rawResponse);
      String jsonText =
          root.path("candidates")
              .path(0)
              .path("content")
              .path("parts")
              .path(0)
              .path("text")
              .asText();

      if (jsonText.isBlank()) {
        log.error("Gemini 응답에서 텍스트를 찾을 수 없음");
        throw new BaseException(ErrorCode.LLM_RESPONSE_INVALID);
      }

      // JSON 객체 범위만 추출 (responseSchema가 있어도 보험으로 유지)
      int startIndex = jsonText.indexOf("{");
      int endIndex = jsonText.lastIndexOf("}");
      if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
        jsonText = jsonText.substring(startIndex, endIndex + 1);
      }

      return jsonText;
    } catch (JsonProcessingException e) {
      log.error("Gemini 응답 JSON 파싱 실패: {}", e.getMessage());
      throw new BaseException(ErrorCode.LLM_RESPONSE_INVALID);
    }
  }
}
