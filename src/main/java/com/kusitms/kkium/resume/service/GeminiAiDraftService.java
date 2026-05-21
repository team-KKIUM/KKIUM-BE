package com.kusitms.kkium.resume.service;

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
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdQuestion;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class GeminiAiDraftService {

  private static final String GEMINI_URL =
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final WebClient webClient;
  private final String apiKey;

  public GeminiAiDraftService(WebClient webClient, @Value("${gemini.api-key}") String apiKey) {
    this.webClient = webClient;
    this.apiKey = apiKey;
  }

  public String generateAiDraft(Jd jd, JdQuestion question, List<Experience> experiences) {
    String prompt = buildPrompt(jd, question, experiences);
    String raw = callGemini(prompt);
    return extractText(raw);
  }

  private String buildPrompt(Jd jd, JdQuestion question, List<Experience> experiences) {
    String expList = buildExperienceList(experiences);
    return """
        너는 자기소개서 초안을 대신 써주는 전문 작가이다.

        [규칙]
        - 반드시 제공된 경험 내용만 근거로 사용하라.
        - 없는 사실, 수치, 성과를 절대 지어내지 마라.
        - 자소서 문항에 대한 완성된 답변 본문을 한 번에 작성하라.
        - JSON이나 마크다운 없이 자소서 본문 텍스트만 반환하라.
        - 한국어로 작성하고, 모든 문장은 '~했습니다', '~입니다' 체로 통일하라.
        - 400자 이상 600자 이내로 작성하라.

        [공고 정보]
        - 기업: %s
        - 직무: %s
        - 주요 업무: %s
        - 필수 역량: %s
        - 우대 역량: %s

        [자소서 문항]
        %s

        [선택된 경험 목록]
        %s

        위 경험들을 자연스럽게 녹여 자소서 문항에 대한 완성된 답변을 작성하라.
        경험의 Situation/Task/Action/Result를 문맥에 맞게 활용하되, 하나의 일관된 이야기 흐름으로 작성하라.
        공고의 요구 역량과 경험이 어떻게 연결되는지 드러나도록 작성하라.
        """
        .formatted(
            jd.getCompanyName(),
            jd.getRecruitmentField(),
            jd.getMainResponsibilities(),
            jd.getRequiredQualifications(),
            jd.getPreferredQualifications(),
            question.getContent(),
            expList);
  }

  private String buildExperienceList(List<Experience> experiences) {
    return IntStream.range(0, experiences.size())
        .mapToObj(
            i -> {
              Experience e = experiences.get(i);
              return """
              경험 %d: %s
              - 한줄소개: %s
              - Situation: %s
              - Task: %s
              - Action: %s
              - Result: %s
              - Taken: %s
              """
                  .formatted(
                      i + 1,
                      e.getTitle(),
                      e.getOneLineIntro(),
                      e.getSituation(),
                      e.getTask(),
                      e.getAct(),
                      e.getResult(),
                      e.getTaken());
            })
        .collect(Collectors.joining("\n"));
  }

  private String callGemini(String prompt) {
    Map<String, Object> body =
        Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
    try {
      return webClient
          .post()
          .uri(GEMINI_URL + "?key=" + apiKey)
          .bodyValue(body)
          .retrieve()
          .bodyToMono(String.class)
          .block();
    } catch (Exception e) {
      log.error("Gemini API 호출 실패: {}", e.getMessage());
      throw new BaseException(ErrorCode.LLM_CALL_FAILED);
    }
  }

  private String extractText(String raw) {
    try {
      JsonNode root = OBJECT_MAPPER.readTree(raw);
      return root.path("candidates")
          .path(0)
          .path("content")
          .path("parts")
          .path(0)
          .path("text")
          .asText();
    } catch (Exception e) {
      log.error("Gemini 응답 파싱 실패: {}", e.getMessage());
      throw new BaseException(ErrorCode.LLM_CALL_FAILED);
    }
  }
}
