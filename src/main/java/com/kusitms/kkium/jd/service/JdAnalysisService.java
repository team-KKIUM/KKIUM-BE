package com.kusitms.kkium.jd.service;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.repository.JdEmbeddingRepository;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.jd.utils.LlmEmbeddingService;
import com.kusitms.kkium.jd.utils.LlmJdAnalyzer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JdAnalysisService {

  private final LlmJdAnalyzer llmJdAnalyzer;
  private final LlmEmbeddingService llmEmbeddingService;
  private final JdRepository jdRepository;
  private final JdEmbeddingRepository jdEmbeddingRepository;

  @Async("jdAnalysisExecutor")
  @Transactional
  public void analyzeAndUpdate(Long jdId, String content) {
    try {
      Jd jd =
          jdRepository
              .findById(jdId)
              .orElseThrow(() -> new IllegalArgumentException("JD를 찾을 수 없습니다: " + jdId));

      LlmJdAnalyzer.AnalyzedJd analyzed = llmJdAnalyzer.analyze(content);

      jd.updateAnalysis(
          analyzed.mainResponsibilities(),
          analyzed.requiredQualifications(),
          analyzed.preferredQualifications(),
          analyzed.hardSkill(),
          analyzed.softSkill());

      String embeddingText = buildEmbeddingText(analyzed);
      if (embeddingText != null) {
        float[] embedding = llmEmbeddingService.embed(embeddingText);
        if (embedding != null) {
          jdEmbeddingRepository.saveEmbedding(jdId, embedding);
        }
      }

      log.info("JD 분석 및 임베딩 완료 - jdId: {}", jdId);
    } catch (Exception e) {
      log.warn("JD 분석 실패 - jdId: {}, 원인: {}", jdId, e.getMessage());
    }
  }

  private String buildEmbeddingText(LlmJdAnalyzer.AnalyzedJd analyzed) {
    String text =
        Stream.of(
                analyzed.hardSkill(),
                analyzed.softSkill(),
                analyzed.mainResponsibilities(),
                analyzed.requiredQualifications(),
                analyzed.preferredQualifications())
            .filter(s -> s != null && !s.isBlank())
            .collect(Collectors.joining("\n"));
    return text.isBlank() ? null : text;
  }
}
