package com.kusitms.kkium.experience.service.llm.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kusitms.kkium.experience.dto.response.ExperienceAnalyzeResponse;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;

class ExperienceResponseParserTest {

  private ObjectMapper objectMapper;
  private ExperienceResponseParser parser;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    parser = new ExperienceResponseParser(objectMapper);
  }

  private String wrapGeminiResponse(String innerText) throws JsonProcessingException {
    return objectMapper.writeValueAsString(
        Map.of(
            "candidates",
            List.of(Map.of("content", Map.of("parts", List.of(Map.of("text", innerText)))))));
  }

  @Test
  @DisplayName("정상 Gemini 응답을 ExperienceAnalyzeResponse로 파싱한다")
  void parse_정상_응답() throws JsonProcessingException {
    String innerJson =
        """
        {
          "title": "프로젝트 A",
          "oneLineIntro": "백엔드 개발",
          "situation": "상황",
          "task": "과제",
          "act": "행동",
          "result": "결과",
          "taken": "배운점",
          "tags": [
            {"category": "TECH", "field": "Java"}
          ]
        }
        """;
    String rawResponse = wrapGeminiResponse(innerJson);

    ExperienceAnalyzeResponse result = parser.parse(rawResponse);

    assertThat(result).isNotNull();
    assertThat(result.title()).isEqualTo("프로젝트 A");
    assertThat(result.oneLineIntro()).isEqualTo("백엔드 개발");
    assertThat(result.tags()).hasSize(1);
  }

  @Test
  @DisplayName("text 앞뒤에 markdown fence가 있어도 JSON만 추출하여 파싱한다")
  void parse_markdown_fence_포함() throws JsonProcessingException {
    String innerText = "```json\n{\"title\": \"제목\"}\n```";
    String rawResponse = wrapGeminiResponse(innerText);

    ExperienceAnalyzeResponse result = parser.parse(rawResponse);

    assertThat(result).isNotNull();
    assertThat(result.title()).isEqualTo("제목");
  }

  @Test
  @DisplayName("rawResponse가 null이면 LLM_RESPONSE_INVALID 예외를 던진다")
  void parse_null_입력() {
    assertThatThrownBy(() -> parser.parse(null))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.LLM_RESPONSE_INVALID);
  }

  @Test
  @DisplayName("rawResponse가 빈 문자열이면 LLM_RESPONSE_INVALID 예외를 던진다")
  void parse_blank_입력() {
    assertThatThrownBy(() -> parser.parse("   "))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.LLM_RESPONSE_INVALID);
  }

  @Test
  @DisplayName("rawResponse가 깨진 JSON 형식이면 LLM_RESPONSE_INVALID 예외를 던진다")
  void parse_깨진_JSON() {
    String broken = "{not a valid json";

    assertThatThrownBy(() -> parser.parse(broken))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.LLM_RESPONSE_INVALID);
  }

  @Test
  @DisplayName("Gemini 응답에 candidates 텍스트가 비어있으면 LLM_RESPONSE_INVALID 예외를 던진다")
  void parse_candidates_텍스트_없음() {
    String rawResponse = "{\"candidates\": []}";

    assertThatThrownBy(() -> parser.parse(rawResponse))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.LLM_RESPONSE_INVALID);
  }

  @Test
  @DisplayName("응답 JSON이 도메인 스키마를 위반하면 LLM_SCHEMA_VIOLATION 예외를 던진다")
  void parse_스키마_위반() throws JsonProcessingException {
    // teamNum은 Integer인데 숫자가 아닌 문자열로 들어옴
    String innerJson =
        """
        {
          "title": "제목",
          "activityInfo": {"teamNum": "not-a-number"}
        }
        """;
    String rawResponse = wrapGeminiResponse(innerJson);

    assertThatThrownBy(() -> parser.parse(rawResponse))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.LLM_SCHEMA_VIOLATION);
  }

  @Test
  @DisplayName("봉투는 정상이나 내부 text의 JSON 구조가 깨지면 LLM_RESPONSE_INVALID 예외를 던진다")
  void parse_내부_JSON_구조_깨짐() throws JsonProcessingException {
    // 봉투는 정상이지만 내부 text가 JSON 구조 자체가 깨진 경우 (value 누락)
    String innerText = "{\"key\": }";
    String rawResponse = wrapGeminiResponse(innerText);

    assertThatThrownBy(() -> parser.parse(rawResponse))
        .isInstanceOf(BaseException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.LLM_RESPONSE_INVALID);
  }
}
