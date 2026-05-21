package com.kusitms.kkium.resume.utils.llm;

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
        너는 10년 경력의 대기업 채용담당자 출신 자기소개서 컨설턴트이다.
        수천 개의 자소서를 검토하며 어떤 글이 합격하고 탈락하는지 정확히 안다.

        [절대 규칙]
        - 제공된 경험 데이터에 없는 사실, 수치, 성과를 절대 지어내지 마라.
        - 마크다운, JSON, 제목, 번호 매기기, 내부 분석 내용 없이 자소서 본문 텍스트만 반환하라.
        - 모든 문장은 '~했습니다', '~입니다' 체로 통일하라.
        - 500자 내외로 작성하라.
        - "~을 통해", "열정", "도전", "최선을 다해", "~하게 되었습니다" 같은 진부한 표현은 절대 사용하지 마라.

        [작성 전 내부 분석 - 절대 출력하지 마라]
        1. 이 문항이 실제로 평가하려는 역량이 무엇인지 파악하라.
        2. 제공된 경험 중 해당 역량을 가장 잘 보여주는 Action과 Result가 무엇인지 선별하라.
        3. 경험이 여러 개라면 시간순 또는 인과관계로 자연스럽게 연결할 방법을 구상하라.
        4. 위 분석을 바탕으로 아래 자소서를 작성하라.

        [작성 방식]
        - 문항이 묻는 핵심에 정확히 답하는 것을 최우선으로 하라.
        - 경험을 나열하지 말고 하나의 일관된 이야기 흐름으로 녹여내라.
        - 구체적인 Action과 Result 중심으로 서술하고, 수치나 성과가 있으면 반드시 포함하라.
        - 마지막 문장에서 이 경험이 지원 직무와 어떻게 연결되는지 자연스럽게 마무리하라.

        [공고 정보]
        - 기업: %s
        - 직무: %s
        - 주요 업무: %s
        - 필수 역량: %s
        - 우대 역량: %s

        [자소서 문항]
        %s

        [활용할 경험]
        %s
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
