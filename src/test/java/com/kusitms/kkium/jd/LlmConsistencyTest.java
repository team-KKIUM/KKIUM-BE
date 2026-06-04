package com.kusitms.kkium.jd;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.repository.ExperienceRepository;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.jd.utils.llm.JdMatchPromptBuilder;

/**
 * LLM 점수 일관성(Consistency) 테스트
 *
 * <p>동일한 JD + 경험 조합으로 OpenAI API를 반복 호출해 temperature별 점수 변동계수(CV)를 측정합니다.
 *
 * <p>사용법: 1. 아래 TARGET_JD_ID, TARGET_USER_ID를 실제 DB 데이터에 맞게 수정 2. local 프로파일로 실행 (DB 연결 필요) 3.
 * OpenAI API 키가 환경변수에 설정되어 있어야 함
 */
@SpringBootTest
@ActiveProfiles("local")
class LlmConsistencyTest {

  // ========== 여기만 수정하세요 ==========
  private static final Long TARGET_JD_ID = 1L; // 테스트할 JD ID
  private static final Long TARGET_USER_ID = 1L; // 테스트할 유저 ID
  private static final int TRIAL_COUNT = 10; // 반복 횟수 (비교군 확보를 위해 10회)
  // =====================================

  private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
  private static final String MODEL = "gpt-4o";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Autowired private JdRepository jdRepository;
  @Autowired private ExperienceRepository experienceRepository;
  @Autowired private JdMatchPromptBuilder promptBuilder;
  @Autowired private WebClient webClient;

  @Value("${openai.api.key}")
  private String apiKey;

  @Disabled("수동 실행 전용 - 로컬 DB, OpenAI API 호출 필요")
  @Test
  @DisplayName("LLM 점수 일관성 테스트: temperature 1.0 vs 0 비교")
  void llmConsistencyTest() throws Exception {
    // 1. 데이터 준비
    Jd jd =
        jdRepository
            .findById(TARGET_JD_ID)
            .orElseThrow(() -> new RuntimeException("JD not found: " + TARGET_JD_ID));
    List<Experience> experiences = experienceRepository.findAllByUserIdForAnalysis(TARGET_USER_ID);
    assertThat(experiences).isNotEmpty();

    String prompt = promptBuilder.buildCombinedPrompt(jd, experiences);
    Map<String, Object> schema = promptBuilder.buildCombinedSchema();

    List<Long> pieceIds = experiences.stream().map(e -> e.getPiece().getId()).toList();

    System.out.println("=".repeat(70));
    System.out.println("LLM 점수 일관성 테스트");
    System.out.println("JD: " + jd.getPostingTitle() + " (id=" + TARGET_JD_ID + ")");
    System.out.println("경험 수: " + experiences.size());
    System.out.println("반복 횟수: " + TRIAL_COUNT);
    System.out.println("=".repeat(70));

    // 2. temperature = 1.0 (현재 기본값)
    System.out.println("\n[ temperature = 1.0 (현재 설정) ]");
    Map<Long, List<Integer>> temp1Scores = runTrials(prompt, schema, pieceIds, 1.0);
    List<Integer> temp1AppScores = new ArrayList<>();
    // applicationScore는 별도 수집 필요 → runTrialsWithAppScore 사용
    Map<Long, List<Integer>> temp1ScoresFull = new HashMap<>();
    List<Integer> appScores1 = new ArrayList<>();
    for (int i = 0; i < TRIAL_COUNT; i++) {
      Map<String, Object> result = callOpenAiAndParse(prompt, schema, 1.0);
      @SuppressWarnings("unchecked")
      Map<Long, Integer> usageScores = (Map<Long, Integer>) result.get("usageScores");
      int appScore = (int) result.get("applicationScore");
      appScores1.add(appScore);
      for (Long pieceId : pieceIds) {
        temp1ScoresFull
            .computeIfAbsent(pieceId, k -> new ArrayList<>())
            .add(usageScores.getOrDefault(pieceId, -1));
      }
      System.out.printf("  Trial %d: appScore=%d | usage=%s%n", i + 1, appScore, usageScores);
      if (i < TRIAL_COUNT - 1) Thread.sleep(3000); // 레이트 리밋 방지
    }
    printResults("temperature=1.0", temp1ScoresFull, appScores1);

    System.out.println("\n  ⏳ 레이트 리밋 방지를 위해 10초 대기...");
    Thread.sleep(10000);

    // 3. temperature = 0
    System.out.println("\n[ temperature = 0 ]");
    Map<Long, List<Integer>> temp0ScoresFull = new HashMap<>();
    List<Integer> appScores0 = new ArrayList<>();
    for (int i = 0; i < TRIAL_COUNT; i++) {
      Map<String, Object> result = callOpenAiAndParse(prompt, schema, 0);
      @SuppressWarnings("unchecked")
      Map<Long, Integer> usageScores = (Map<Long, Integer>) result.get("usageScores");
      int appScore = (int) result.get("applicationScore");
      appScores0.add(appScore);
      for (Long pieceId : pieceIds) {
        temp0ScoresFull
            .computeIfAbsent(pieceId, k -> new ArrayList<>())
            .add(usageScores.getOrDefault(pieceId, -1));
      }
      System.out.printf("  Trial %d: appScore=%d | usage=%s%n", i + 1, appScore, usageScores);
      if (i < TRIAL_COUNT - 1) Thread.sleep(3000); // 레이트 리밋 방지
    }
    printResults("temperature=0", temp0ScoresFull, appScores0);

    // 4. 비교 요약
    System.out.println("\n" + "=".repeat(70));
    System.out.println("비교 요약");
    System.out.println("=".repeat(70));
    double avgCv1 = calcAverageCv(temp1ScoresFull);
    double avgCv0 = calcAverageCv(temp0ScoresFull);
    System.out.printf("  temp=1.0 평균 CV: %.1f%%%n", avgCv1);
    System.out.printf("  temp=0   평균 CV: %.1f%%%n", avgCv0);
    System.out.printf(
        "  applicationScore CV: temp=1.0 → %.1f%% | temp=0 → %.1f%%%n",
        calcCv(appScores1), calcCv(appScores0));
    System.out.println();
    System.out.println("판단 기준:");
    System.out.println("  CV ≤ 5%   → 안정적 (LLM 점수 신뢰 가능)");
    System.out.println("  CV 5~15%  → 보통 (가중치 0.3이면 수용 가능)");
    System.out.println("  CV > 15%  → 불안정 (temperature 0 적용 또는 프롬프트 개선 필요)");
  }

  // ====== 헬퍼 메서드 ======

  private Map<String, Object> callOpenAiAndParse(
      String prompt, Map<String, Object> schema, double temperature) throws Exception {

    Map<String, Object> body = new HashMap<>();
    body.put("model", MODEL);
    body.put("temperature", temperature);
    body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
    body.put("response_format", Map.of("type", "json_schema", "json_schema", schema));

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

    JsonNode root = MAPPER.readTree(response);
    String content = root.path("choices").path(0).path("message").path("content").asText();
    JsonNode parsed = MAPPER.readTree(content);

    Map<Long, Integer> usageScores = new HashMap<>();
    for (JsonNode item : parsed.path("usageScores")) {
      usageScores.put(
          item.path("pieceId").asLong(), Math.max(0, Math.min(100, item.path("score").asInt(50))));
    }
    int appScore = Math.max(0, Math.min(100, parsed.path("applicationScore").asInt(0)));

    Map<String, Object> result = new HashMap<>();
    result.put("usageScores", usageScores);
    result.put("applicationScore", appScore);
    return result;
  }

  private Map<Long, List<Integer>> runTrials(
      String prompt, Map<String, Object> schema, List<Long> pieceIds, double temperature) {
    // 실제 호출은 위의 루프에서 수행, 이 메서드는 사용하지 않음
    return new HashMap<>();
  }

  private void printResults(
      String label, Map<Long, List<Integer>> scoresByPiece, List<Integer> appScores) {
    System.out.println("\n  ── " + label + " 결과 ──");
    System.out.printf(
        "  %-10s | %-30s | %6s | %6s | %6s%n", "pieceId", "점수들", "평균", "표준편차", "CV(%)");
    System.out.println("  " + "-".repeat(75));

    for (Map.Entry<Long, List<Integer>> entry : scoresByPiece.entrySet()) {
      List<Integer> scores = entry.getValue();
      double mean = scores.stream().mapToInt(i -> i).average().orElse(0);
      double std = calcStd(scores, mean);
      double cv = mean > 0 ? (std / mean) * 100 : 0;
      System.out.printf(
          "  %-10d | %-30s | %6.1f | %6.1f | %5.1f%%%n", entry.getKey(), scores, mean, std, cv);
    }

    double appMean = appScores.stream().mapToInt(i -> i).average().orElse(0);
    double appStd = calcStd(appScores, appMean);
    double appCv = appMean > 0 ? (appStd / appMean) * 100 : 0;
    System.out.printf(
        "  %-10s | %-30s | %6.1f | %6.1f | %5.1f%%%n",
        "appScore", appScores, appMean, appStd, appCv);
  }

  private double calcStd(List<Integer> values, double mean) {
    double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
    return Math.sqrt(variance);
  }

  private double calcCv(List<Integer> values) {
    double mean = values.stream().mapToInt(i -> i).average().orElse(0);
    if (mean == 0) return 0;
    double std = calcStd(values, mean);
    return (std / mean) * 100;
  }

  private double calcAverageCv(Map<Long, List<Integer>> scoresByPiece) {
    return scoresByPiece.values().stream()
        .mapToDouble(
            scores -> {
              double mean = scores.stream().mapToInt(i -> i).average().orElse(0);
              if (mean == 0) return 0;
              return (calcStd(scores, mean) / mean) * 100;
            })
        .average()
        .orElse(0);
  }
}
