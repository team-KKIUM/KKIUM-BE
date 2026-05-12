package com.kusitms.kkium.jd.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.jd.utils.LlmJdAnalyzer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JdAnalysisService {

  private final LlmJdAnalyzer llmJdAnalyzer;
  private final JdRepository jdRepository;

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

      log.info("JD 분석 완료 - jdId: {}", jdId);
    } catch (Exception e) {
      log.warn("JD 분석 실패 - jdId: {}, 원인: {}", jdId, e.getMessage());
    }
  }
}
