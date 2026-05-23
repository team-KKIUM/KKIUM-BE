package com.kusitms.kkium.home.service.llm;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.repository.ExperienceRepository;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;
import com.kusitms.kkium.user.domain.type.JobType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiJobTypeLlmService implements JobTypeLlmService {

  private static final String GEMINI_API_URL =
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

  private final WebClient webClient;
  private final ObjectMapper objectMapper;
  private final ExperienceRepository experienceRepository;

  @Value("${gemini.api-key}")
  private String apiKey;

  @Override
  public JobType analyzeJobType(Long userId) {
    List<Experience> experiences = experienceRepository.findAllByUserIdNoPage(userId);

    if (experiences.isEmpty()) {
      return null;
    }

    String experienceText = buildExperienceText(experiences);
    String prompt = buildPrompt(experienceText);
    String rawJson = callGeminiApi(prompt);
    return parseJobType(rawJson);
  }

  private String buildExperienceText(List<Experience> experiences) {
    return experiences.stream()
        .map(
            e ->
                """
                [경험: %s]
                상황: %s
                과제: %s
                행동: %s
                결과: %s
                배운점: %s
                """
                    .formatted(
                        e.getTitle(),
                        nullSafe(e.getSituation()),
                        nullSafe(e.getTask()),
                        nullSafe(e.getAct()),
                        nullSafe(e.getResult()),
                        nullSafe(e.getTaken())))
        .collect(Collectors.joining("\n"));
  }

  private String buildPrompt(String experienceText) {
    return """
        아래는 사용자의 전체 경험 목록입니다.
        경험들에서 반복적으로 나타나는 역량과 패턴을 분석하여, 아래 9가지 직무 유형 중 가장 잘 부합하는 하나를 선택해 주세요.
        JSON 외 다른 텍스트는 절대 포함하지 마세요.

        직무 유형:
        - GOAL_DESIGNER: 목표 설계자 (전략적 실행·계획력)
        - DRIVEN_EXECUTOR: 추진형 실행가 (추진력·빠른 결단)
        - PRECISION_ANALYST: 정밀 분석가 (데이터 기반 사고·정확성)
        - RELATIONSHIP_CONNECTOR: 관계 연결자 (공감·소통·팀 화합)
        - STABLE_SUPPORTER: 안정적 지지자 (일관성·신뢰감·서포터)
        - IDEA_EXPLORER: 아이디어 탐험가 (창의성·새로운 시각)
        - PRINCIPLE_GUARDIAN: 원칙 수호자 (윤리·완벽주의·기준 준수)
        - GROWTH_SEEKER: 성장 지향자 (학습·자기계발·피드백 수용)
        - BALANCE_COORDINATOR: 균형 조율자 (중재·유연성·맥락 파악)

        경험 목록:
        %s

        응답 형식:
        {
          "jobType": "GOAL_DESIGNER"
        }
        """
        .formatted(experienceText);
  }

  private String callGeminiApi(String prompt) {
    Map<String, Object> requestBody =
        Map.of(
            "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
            "generationConfig", Map.of("response_mime_type", "application/json"));

    try {
      return webClient
          .post()
          .uri(GEMINI_API_URL + "?key=" + apiKey)
          .bodyValue(requestBody)
          .retrieve()
          .bodyToMono(String.class)
          .block();
    } catch (Exception e) {
      log.error("[직무유형] Gemini API 호출 실패: {}", e.getMessage());
      throw new BaseException(ErrorCode.LLM_CALL_FAILED);
    }
  }

  private JobType parseJobType(String rawResponse) {
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

      int startIndex = jsonText.indexOf("{");
      int endIndex = jsonText.lastIndexOf("}");
      if (startIndex != -1 && endIndex != -1) {
        jsonText = jsonText.substring(startIndex, endIndex + 1);
      }

      JsonNode parsed = objectMapper.readTree(jsonText);
      String jobTypeName = parsed.path("jobType").asText();
      try {
        return JobType.valueOf(jobTypeName);
      } catch (IllegalArgumentException e) {
        log.error("[직무유형] 알 수 없는 JobType 값: {}", jobTypeName);
        throw new BaseException(ErrorCode.LLM_CALL_FAILED);
      }
    } catch (Exception e) {
      log.error("[직무유형] 응답 파싱 실패: {}", e.getMessage());
      throw new BaseException(ErrorCode.LLM_CALL_FAILED);
    }
  }

  private String nullSafe(String value) {
    return value != null ? value : "";
  }
}
