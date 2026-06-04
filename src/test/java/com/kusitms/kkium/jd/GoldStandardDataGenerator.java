package com.kusitms.kkium.jd;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

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
import com.kusitms.kkium.jd.repository.JdMatchRepository;
import com.kusitms.kkium.jd.repository.JdMatchRepository.PieceSimilarity;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.jd.utils.llm.JdMatchPromptBuilder;

/**
 * Gold Standard 평가 데이터셋 생성기
 *
 * <p>DB에서 JD + 경험 데이터를 뽑고, 임베딩/LLM 시스템 점수를 수집해서 팀원 평가용 CSV 파일을 생성합니다.
 *
 * <p>생성된 CSV를 구글 시트에 임포트 → 팀원 3명이 각자 "평가자 점수" 열을 채움 → 시스템 점수와 비교해서 가중치 최적화
 *
 * <p>출력 파일: - gold_standard_jd_info.csv (평가자가 참고할 JD 정보) - gold_standard_eval.csv (평가 시트)
 */
@SpringBootTest
@ActiveProfiles("local")
class GoldStandardDataGenerator {

  // ========== 여기만 수정하세요 ==========
  private static final Long TARGET_JD_ID = 25L;
  private static final Long TARGET_USER_ID = 1L;
  private static final String OUTPUT_DIR = "build/gold-standard/";
  // =====================================

  private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";
  private static final String MODEL = "gpt-4o";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Autowired private JdRepository jdRepository;
  @Autowired private ExperienceRepository experienceRepository;
  @Autowired private JdMatchRepository jdMatchRepository;
  @Autowired private JdMatchPromptBuilder promptBuilder;
  @Autowired private WebClient webClient;

  @Value("${openai.api.key}")
  private String apiKey;

  @Disabled("수동 실행 전용 - 로컬 DB, OpenAI API 호출 필요")
  @Test
  @DisplayName("Gold Standard 평가용 CSV 생성")
  void generateGoldStandardCsv() throws Exception {

    // 1. 데이터 조회
    Jd jd =
        jdRepository.findById(TARGET_JD_ID).orElseThrow(() -> new RuntimeException("JD not found"));
    List<Experience> experiences = experienceRepository.findAllByUserIdForAnalysis(TARGET_USER_ID);
    List<PieceSimilarity> similarities =
        jdMatchRepository.findSimilaritiesByJdAndUser(TARGET_JD_ID, TARGET_USER_ID);

    // pieceId → 임베딩 점수 맵
    Map<Long, Integer> embScores =
        similarities.stream()
            .collect(Collectors.toMap(PieceSimilarity::pieceId, PieceSimilarity::similarityScore));

    // 2. LLM 점수 수집 (temperature=0)
    System.out.println("LLM 점수 수집 중 (temperature=0)...");
    String prompt = promptBuilder.buildCombinedPrompt(jd, experiences);
    Map<String, Object> schema = promptBuilder.buildCombinedSchema();
    Map<Long, Integer> llmScores = callLlm(prompt, schema);

    // 3. 출력 디렉토리 생성
    new java.io.File(OUTPUT_DIR).mkdirs();

    // 4. JD 정보 CSV (평가자 참고용)
    String jdFile = OUTPUT_DIR + "gold_standard_jd_info.csv";
    try (PrintWriter pw = new PrintWriter(new FileWriter(jdFile))) {
      pw.println("항목,내용");
      pw.println(csvCell("공고 제목") + "," + csvCell(jd.getPostingTitle()));
      pw.println(csvCell("회사명") + "," + csvCell(jd.getCompanyName()));
      pw.println(csvCell("모집 분야") + "," + csvCell(jd.getRecruitmentField()));
      pw.println(csvCell("주요 업무") + "," + csvCell(jd.getMainResponsibilities()));
      pw.println(csvCell("자격 요건") + "," + csvCell(jd.getRequiredQualifications()));
      pw.println(csvCell("우대 사항") + "," + csvCell(jd.getPreferredQualifications()));
      pw.println(csvCell("Hard Skills") + "," + csvCell(jd.getHardSkill()));
      pw.println(csvCell("Soft Skills") + "," + csvCell(jd.getSoftSkill()));
    }
    System.out.println("JD 정보 → " + jdFile);

    // 5. 평가 시트 CSV
    String evalFile = OUTPUT_DIR + "gold_standard_eval.csv";
    try (PrintWriter pw = new PrintWriter(new FileWriter(evalFile))) {
      // 헤더
      pw.println(
          String.join(
              ",",
              csvCell("경험ID"),
              csvCell("경험 제목"),
              csvCell("한줄 소개"),
              csvCell("상황(S)"),
              csvCell("과제(T)"),
              csvCell("행동(A)"),
              csvCell("결과(R)"),
              csvCell("배운점"),
              csvCell("[시스템] 임베딩 점수"),
              csvCell("[시스템] LLM 점수"),
              csvCell("[시스템] 최종 점수 (emb*0.7+llm*0.3)"),
              csvCell("평가자A (0~100)"),
              csvCell("평가자B (0~100)"),
              csvCell("평가자C (0~100)"),
              csvCell("평가자 평균")));

      // 경험별 행
      for (Experience exp : experiences) {
        Long pieceId = exp.getPiece().getId();
        int emb = embScores.getOrDefault(pieceId, 0);
        int llm = llmScores.getOrDefault(pieceId, 50);
        int finalScore = (int) Math.round(emb * 0.7 + llm * 0.3);

        pw.println(
            String.join(
                ",",
                csvCell(String.valueOf(exp.getId())),
                csvCell(exp.getTitle()),
                csvCell(exp.getOneLineIntro()),
                csvCell(exp.getSituation()),
                csvCell(exp.getTask()),
                csvCell(exp.getAct()),
                csvCell(exp.getResult()),
                csvCell(exp.getTaken()),
                csvCell(String.valueOf(emb)),
                csvCell(String.valueOf(llm)),
                csvCell(String.valueOf(finalScore)),
                "",
                "",
                "",
                csvCell(
                    "=AVERAGE(L{row}:N{row})"
                        .replace("{row}", String.valueOf(experiences.indexOf(exp) + 2)))));
      }
    }
    System.out.println("평가 시트 → " + evalFile);

    // 6. 요약 출력
    System.out.println("\n" + "=".repeat(60));
    System.out.println("Gold Standard 데이터 생성 완료");
    System.out.println("=".repeat(60));
    System.out.printf("JD: %s (%s)%n", jd.getPostingTitle(), jd.getCompanyName());
    System.out.printf("경험 수: %d개%n", experiences.size());
    System.out.println();
    System.out.printf("%-4s | %-30s | %5s | %5s | %5s%n", "ID", "경험 제목", "임베딩", "LLM", "최종");
    System.out.println("-".repeat(80));
    for (Experience exp : experiences) {
      Long pieceId = exp.getPiece().getId();
      int emb = embScores.getOrDefault(pieceId, 0);
      int llm = llmScores.getOrDefault(pieceId, 50);
      int finalScore = (int) Math.round(emb * 0.7 + llm * 0.3);
      System.out.printf(
          "%-4d | %-30s | %5d | %5d | %5d%n",
          exp.getId(), truncate(exp.getTitle(), 30), emb, llm, finalScore);
    }
    System.out.println();
    System.out.println("사용 방법:");
    System.out.println("1. gold_standard_eval.csv를 구글 시트에 임포트");
    System.out.println("2. gold_standard_jd_info.csv를 참고하면서");
    System.out.println("3. 평가자A/B/C 열에 각자 0~100 점수 입력");
    System.out.println("4. 시스템 점수 열은 평가 후에 공개 (편향 방지)");
  }

  // ====== 헬퍼 ======

  private Map<Long, Integer> callLlm(String prompt, Map<String, Object> schema) throws Exception {
    Map<String, Object> body = new HashMap<>();
    body.put("model", MODEL);
    body.put("temperature", 0);
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

    Map<Long, Integer> scores = new HashMap<>();
    for (JsonNode item : parsed.path("usageScores")) {
      scores.put(
          item.path("pieceId").asLong(), Math.max(0, Math.min(100, item.path("score").asInt(50))));
    }
    return scores;
  }

  private String csvCell(String value) {
    if (value == null) return "\"\"";
    return "\"" + value.replace("\"", "\"\"").replace("\n", " | ") + "\"";
  }

  private String truncate(String s, int maxLen) {
    if (s == null) return "";
    return s.length() <= maxLen ? s : s.substring(0, maxLen - 2) + "..";
  }
}
