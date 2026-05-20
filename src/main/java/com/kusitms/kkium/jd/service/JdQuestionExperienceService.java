package com.kusitms.kkium.jd.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.JD_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.NOT_FOUND;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.repository.ExperienceRepository;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.jd.dto.response.JdQuestionExperienceResponse;
import com.kusitms.kkium.jd.dto.response.JdQuestionExperienceResponse.ExperienceMatchItem;
import com.kusitms.kkium.jd.repository.JdMatchRepository;
import com.kusitms.kkium.jd.repository.JdMatchRepository.PieceSimilarity;
import com.kusitms.kkium.jd.repository.JdQuestionRepository;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.jd.utils.llm.LlmMatchScoreService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JdQuestionExperienceService {

  private final JdRepository jdRepository;
  private final JdQuestionRepository jdQuestionRepository;
  private final ExperienceRepository experienceRepository;
  private final JdMatchRepository jdMatchRepository;
  private final LlmMatchScoreService llmMatchScoreService;

  public JdQuestionExperienceResponse getExperiencesWithFitScore(
      Long jdId, Long questionId, Long userId) {

    // 1. JD 조회
    Jd jd = jdRepository.findById(jdId).orElseThrow(() -> new BaseException(JD_NOT_FOUND));

    // 2. 문항 조회
    JdQuestion question =
        jdQuestionRepository.findById(questionId).orElseThrow(() -> new BaseException(NOT_FOUND));

    // 3. 유저의 전체 경험 조회
    List<Experience> allExperiences = experienceRepository.findAllByUserIdNoPage(userId);

    if (allExperiences.isEmpty()) {
      return new JdQuestionExperienceResponse(List.of());
    }

    // 4. 임베딩 코사인 유사도 계산 (pieceId → embeddingScore)
    List<PieceSimilarity> similarities =
        jdMatchRepository.findSimilaritiesByJdAndUser(jdId, userId);
    Map<Long, Integer> embeddingScoreMap =
        similarities.stream()
            .collect(
                Collectors.toMap(
                    PieceSimilarity::pieceId,
                    PieceSimilarity::similarityScore,
                    (existing, replacement) -> existing));

    // 5. LLM 호출 — 문항 content를 추가 컨텍스트로 포함
    LlmMatchScoreService.LlmQuestionMatchResult llmResult =
        llmMatchScoreService.scoreAllByQuestion(jd, question, allExperiences);
    Map<Long, Integer> llmScoreMap = llmResult.usageScores();

    // 6. 활용 적합도 계산: 임베딩 × 0.7 + LLM × 0.3
    Map<Long, Integer> usageFitScoreMap = new HashMap<>();
    for (Experience exp : allExperiences) {
      Long pieceId = exp.getPiece().getId();
      int embScore = embeddingScoreMap.getOrDefault(pieceId, 0);
      int llmScore = llmScoreMap.getOrDefault(pieceId, 0);
      int usageFitScore = (int) Math.round(embScore * 0.7 + llmScore * 0.3);
      log.info(
          "[문항별 경험 적합도] pieceId={} | 임베딩={} | LLM={} | 활용적합도={}",
          pieceId,
          embScore,
          llmScore,
          usageFitScore);
      usageFitScoreMap.put(pieceId, usageFitScore);
    }

    // 7. 응답 구성 (usageFitScore 내림차순 정렬)
    List<ExperienceMatchItem> items =
        allExperiences.stream()
            .map(
                exp ->
                    new ExperienceMatchItem(
                        exp.getId(),
                        exp.getTitle(),
                        exp.getOneLineIntro(),
                        usageFitScoreMap.getOrDefault(exp.getPiece().getId(), 0)))
            .sorted(Comparator.comparingInt(ExperienceMatchItem::usageFitScore).reversed())
            .toList();

    return new JdQuestionExperienceResponse(items);
  }
}
