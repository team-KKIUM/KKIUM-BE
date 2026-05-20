package com.kusitms.kkium.jd.utils.llm;

import java.util.ArrayList;
import java.util.HashMap;
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

  private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
  private static final String MODEL = "gpt-4o";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final WebClient webClient;
  private final String apiKey;

  public LlmMatchScoreService(WebClient webClient, @Value("${openai.api.key}") String apiKey) {
    this.webClient = webClient;
    this.apiKey = apiKey;
  }

  /** LLM 1번 호출로 활용 적합도(경험별) + 지원 적합도(포트폴리오 종합)를 한꺼번에 반환 */
  public LlmMatchResult scoreAll(Jd jd, List<Experience> experiences) {
    if (experiences.isEmpty()) return new LlmMatchResult(Map.of(), 0);
    String prompt = buildCombinedPrompt(jd, experiences);
    String response = callOpenAi(prompt);
    return parseCombinedResult(response, experiences);
  }

  public record LlmMatchResult(Map<Long, Integer> usageScores, int applicationScore) {}

  /** 경험 카드 클릭 시 상세 분석 */
  public LlmExperienceDetailResult analyzeExperienceDetail(Jd jd, Experience experience) {
    String prompt = buildExperienceDetailPrompt(jd, experience);
    String response = callOpenAi(prompt);
    return parseExperienceDetailResult(response);
  }

  public record LlmExperienceDetailResult(
      String strengths, String weaknesses, String usageGuide, List<String> highlightKeywords) {

    public static LlmExperienceDetailResult empty() {
      return new LlmExperienceDetailResult("분석에 실패했습니다.", "분석에 실패했습니다.", "분석에 실패했습니다.", List.of());
    }
  }

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
        - 우대 역량: %s
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
            jd.getPreferredQualifications(),
            jd.getHardSkill(),
            jd.getSoftSkill(),
            expList);
  }

  private String buildExperienceDetailPrompt(Jd jd, Experience experience) {
    return """
        너는 채용 공고와 지원자 경험을 비교 분석하는 전문가이다.

        [규칙]
        - 반드시 제공된 내용만 근거로 사용하라.
        - 없는 내용을 추론하거나 만들어내지 마라.
        - 반드시 JSON 형식으로만 응답하라.
        - 한국어로 작성하라.

        [공고 정보]
        - 기업: %s
        - 직무: %s
        - 주요 업무: %s
        - 필수 역량: %s
        - 우대 역량: %s
        - 기술 스택: %s
        - 소프트 스킬: %s

        [경험 정보]
        - 제목: %s
        - 한줄소개: %s
        - Situation: %s
        - Task: %s
        - Action: %s
        - Result: %s
        - Taken: %s

        [해야 할 일]
        1. strengths: 이 경험이 공고 요구사항과 어떻게 직접적으로 연결되는지 2문장 이내로 서술하라.
        2. weaknesses: 이 경험에서 보완하면 공고에 더 잘 어필할 수 있는 점을 2문장 이내로 서술하라. 경험 자체의 아쉬운 점이 아니라 공고 관점에서 추가하면 좋을 내용을 제안하라.
        3. usageGuide: 이 경험을 자기소개서에서 이 공고에 맞게 어떻게 어필할지 2문장 이내로 서술하라.
        4. highlightKeywords: 공고 텍스트에서 이 경험과 직접 연관되는 핵심 키워드를 최대 5개 추출하라.
           반드시 공고의 주요 업무, 필수 역량, 우대 역량, 기술 스택, 소프트 스킬에 실제로 존재하는 단어나 구문만 반환하라.

        [말투 규칙]
        - 모든 문장은 반드시 '~합니다', '~입니다', '~습니다' 체로 통일하라.
        - '~하십시오', '~이다', '~한다', '~세요', '~어요' 등 다른 말투는 절대 사용하지 마라.

        반환 형식:
        {
          "strengths": "좋은 점 서술",
          "weaknesses": "보완하면 좋을 점 서술",
          "usageGuide": "자기소개서 어필 방법 서술",
          "highlightKeywords": ["키워드1", "키워드2", ...]
        }
        """
        .formatted(
            jd.getCompanyName(),
            jd.getRecruitmentField(),
            jd.getMainResponsibilities(),
            jd.getRequiredQualifications(),
            jd.getPreferredQualifications(),
            jd.getHardSkill(),
            jd.getSoftSkill(),
            experience.getTitle(),
            experience.getOneLineIntro(),
            experience.getSituation(),
            experience.getTask(),
            experience.getAct(),
            experience.getResult(),
            experience.getTaken());
  }

  private String callOpenAi(String prompt) {
    Map<String, Object> body =
        Map.of(
            "model", MODEL,
            "messages", List.of(Map.of("role", "user", "content", prompt)),
            "response_format", Map.of("type", "json_object"));
    try {
      String response =
          webClient
              .post()
              .uri(OPENAI_URL)
              .header("Content-Type", "application/json")
              .header("Authorization", "Bearer " + apiKey)
              .bodyValue(body)
              .retrieve()
              .bodyToMono(String.class)
              .block();

      JsonNode root = OBJECT_MAPPER.readTree(response);
      return root.path("choices").path(0).path("message").path("content").asText();
    } catch (Exception e) {
      log.warn("OpenAI 호출 실패: {}", e.getMessage());
      return null;
    }
  }

  private LlmMatchResult parseCombinedResult(String response, List<Experience> experiences) {
    Map<Long, Integer> fallback =
        experiences.stream().collect(Collectors.toMap(e -> e.getPiece().getId(), e -> 50));
    if (response == null) return new LlmMatchResult(fallback, 0);
    try {
      JsonNode parsed = OBJECT_MAPPER.readTree(response);
      Map<Long, Integer> usageScores = new HashMap<>();
      for (JsonNode item : parsed.path("usageScores")) {
        usageScores.put(
            item.path("pieceId").asLong(),
            Math.max(0, Math.min(100, item.path("score").asInt(50))));
      }
      fallback.forEach(usageScores::putIfAbsent);
      return new LlmMatchResult(
          usageScores, Math.max(0, Math.min(100, parsed.path("applicationScore").asInt(0))));
    } catch (Exception e) {
      log.warn("LLM 응답 파싱 실패: {}", e.getMessage());
      return new LlmMatchResult(fallback, 0);
    }
  }

  private LlmExperienceDetailResult parseExperienceDetailResult(String response) {
    if (response == null) return LlmExperienceDetailResult.empty();
    try {
      JsonNode parsed = OBJECT_MAPPER.readTree(response);
      List<String> keywords = new ArrayList<>();
      for (JsonNode k : parsed.path("highlightKeywords")) keywords.add(k.asText());
      return new LlmExperienceDetailResult(
          parsed.path("strengths").asText("분석에 실패했습니다."),
          parsed.path("weaknesses").asText("분석에 실패했습니다."),
          parsed.path("usageGuide").asText("분석에 실패했습니다."),
          keywords);
    } catch (Exception e) {
      log.warn("경험 상세 분석 LLM 응답 파싱 실패: {}", e.getMessage());
      return LlmExperienceDetailResult.empty();
    }
  }
}
