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

      // JSON 객체 범위만 추출
      int startIndex = jsonText.indexOf("{");
      int endIndex = jsonText.lastIndexOf("}");
      if (startIndex != -1 && endIndex != -1) {
        jsonText = jsonText.substring(startIndex, endIndex + 1);
      }

      return objectMapper.readValue(jsonText, ExperienceAnalyzeResponse.class);
    } catch (JsonProcessingException e) {
      log.error("Gemini 응답 파싱 실패: {}", e.getMessage());
      throw new BaseException(ErrorCode.LLM_CALL_FAILED);
    }
  }
}
