package com.kusitms.kkium.jd.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.JD_NOT_FOUND;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.dto.response.TagResponse;
import com.kusitms.kkium.experience.repository.ExperienceRepository;
import com.kusitms.kkium.experience.repository.TagRepository;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.type.AnalysisStatus;
import com.kusitms.kkium.jd.dto.response.JdMatchAnalysisResponse;
import com.kusitms.kkium.jd.dto.response.JdMatchAnalysisResponse.ExperienceMatchCard;
import com.kusitms.kkium.jd.dto.response.JdMatchAnalysisResponse.JdInfo;
import com.kusitms.kkium.jd.dto.response.JdMatchAnalysisResponse.MatchResult;
import com.kusitms.kkium.jd.repository.JdMatchRepository;
import com.kusitms.kkium.jd.repository.JdMatchRepository.PieceSimilarity;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.jd.utils.llm.LlmMatchScoreService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JdMatchService {

  private static final int USAGE_FIT_THRESHOLD = 50; // 활용 가능 경험 임계값
  private static final int TOP_N_FOR_CARD = 5; // 카드로 보여줄 상위 N개
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

  private final JdRepository jdRepository;
  private final ExperienceRepository experienceRepository;
  private final TagRepository tagRepository;
  private final JdMatchRepository jdMatchRepository;
  private final LlmMatchScoreService llmMatchScoreService;

  public JdMatchAnalysisResponse analyze(Long jdId, Long userId) {
    Jd jd = jdRepository.findById(jdId).orElseThrow(() -> new BaseException(JD_NOT_FOUND));

    // 분석 미완료 시 상태만 반환
    if (jd.getAnalysisStatus() != AnalysisStatus.COMPLETED) {
      return new JdMatchAnalysisResponse(jd.getAnalysisStatus(), null, null);
    }

    // 1. JD Info 구성
    JdInfo jdInfo = buildJdInfo(jd);

    // 2. 유저의 전체 경험 조회
    List<Experience> allExperiences = experienceRepository.findAllByUserIdNoPage(userId);

    if (allExperiences.isEmpty()) {
      return new JdMatchAnalysisResponse(
          AnalysisStatus.COMPLETED, jdInfo, new MatchResult(0, 0, List.of()));
    }

    // 3. 임베딩 코사인 유사도 계산 (pieceId → embeddingScore)
    List<PieceSimilarity> similarities =
        jdMatchRepository.findSimilaritiesByJdAndUser(jdId, userId);
    Map<Long, Integer> embeddingScoreMap =
        similarities.stream()
            .collect(Collectors.toMap(PieceSimilarity::pieceId, PieceSimilarity::similarityScore));
    log.info("[공고분석] 전체 임베딩 유사도 점수: {}", embeddingScoreMap);

    // 4. 임베딩 점수 상위 5개 경험 추출
    List<Experience> topExperiences =
        allExperiences.stream()
            .sorted(
                Comparator.comparingInt(
                        (Experience e) -> embeddingScoreMap.getOrDefault(e.getPiece().getId(), 0))
                    .reversed())
            .limit(TOP_N_FOR_CARD)
            .toList();
    log.info(
        "[공고분석] 임베딩 상위 5개 pieceId: {}",
        topExperiences.stream().map(e -> e.getPiece().getId()).toList());

    // 5. LLM 1번 호출 - 활용 적합도 + 지원 적합도 한꺼번에
    LlmMatchScoreService.LlmMatchResult llmResult =
        llmMatchScoreService.scoreAll(jd, topExperiences);
    Map<Long, Integer> llmScoreMap = llmResult.usageScores();
    log.info("[공고분석] 활용 적합도 LLM 점수: {}", llmScoreMap);
    log.info("[공고분석] 지원 적합도 LLM 점수: {}", llmResult.applicationScore());

    // 6. 활용 적합도 계산: 임베딩 × 0.7 + LLM × 0.3 (상위 5개만)
    Map<Long, Integer> usageFitScoreMap = new HashMap<>();
    for (Experience exp : topExperiences) {
      Long pieceId = exp.getPiece().getId();
      int embScore = embeddingScoreMap.getOrDefault(pieceId, 0);
      int llmScore = llmScoreMap.getOrDefault(pieceId, 0);
      int usageFitScore = (int) Math.round(embScore * 0.7 + llmScore * 0.3);
      log.info(
          "[공고분석] pieceId={} | 임베딩={} | LLM={} | 활용적합도={}",
          pieceId,
          embScore,
          llmScore,
          usageFitScore);
      usageFitScoreMap.put(pieceId, usageFitScore);
    }

    // 6. 지원 적합도 계산 (LLM 점수는 이미 위에서 받음)
    int applicationFitScore =
        calcApplicationFitScore(
            topExperiences, embeddingScoreMap, usageFitScoreMap, llmResult.applicationScore());

    // 7. 활용 가능 경험 개수 (활용 적합도 50점 이상, 상위 5개 기준)
    int usableCount =
        (int)
            usageFitScoreMap.values().stream()
                .filter(score -> score >= USAGE_FIT_THRESHOLD)
                .count();

    // 8. 태그 벌크 조회 (상위 5개만)
    List<Long> experienceIds = topExperiences.stream().map(Experience::getId).toList();
    Map<Long, List<TagResponse>> tagMap =
        tagRepository.findByExperienceIdIn(experienceIds).stream()
            .collect(
                Collectors.groupingBy(
                    t -> t.getExperience().getId(),
                    Collectors.mapping(
                        t -> new TagResponse(t.getCategory(), t.getField()), Collectors.toList())));

    // 9. 경험 카드 목록 구성 (상위 5개, 활용 적합도 내림차순)
    List<ExperienceMatchCard> cards =
        topExperiences.stream()
            .map(
                exp -> {
                  Long pieceId = exp.getPiece().getId();
                  return new ExperienceMatchCard(
                      pieceId,
                      exp.getId(),
                      exp.getPiece().getType(),
                      exp.getTitle(),
                      exp.getOneLineIntro(),
                      tagMap.getOrDefault(exp.getId(), List.of()),
                      usageFitScoreMap.getOrDefault(pieceId, 0));
                })
            .sorted(Comparator.comparingInt(ExperienceMatchCard::usageFitScore).reversed())
            .toList();

    return new JdMatchAnalysisResponse(
        AnalysisStatus.COMPLETED, jdInfo, new MatchResult(applicationFitScore, usableCount, cards));
  }

  /** 지원 적합도 계산 상위 5개 임베딩 점수 평균 × 0.7 + LLM 포트폴리오 점수 × 0.3 */
  private int calcApplicationFitScore(
      List<Experience> topExperiences,
      Map<Long, Integer> embeddingScoreMap,
      Map<Long, Integer> usageFitScoreMap,
      int llmApplicationScore) {

    // 활용 적합도 기준으로 정렬
    List<Experience> topForApp =
        topExperiences.stream()
            .sorted(
                Comparator.comparingInt(
                        (Experience e) -> usageFitScoreMap.getOrDefault(e.getPiece().getId(), 0))
                    .reversed())
            .toList();

    // 임베딩 점수 평균
    double avgEmbeddingScore =
        topForApp.stream()
            .mapToInt(e -> embeddingScoreMap.getOrDefault(e.getPiece().getId(), 0))
            .average()
            .orElse(0);

    log.info(
        "[공고분석] 지원 적합도 | 임베딩 평균={} | LLM={} | 최종={}",
        avgEmbeddingScore,
        llmApplicationScore,
        (int) Math.round(avgEmbeddingScore * 0.7 + llmApplicationScore * 0.3));

    return (int) Math.round(avgEmbeddingScore * 0.7 + llmApplicationScore * 0.3);
  }

  private JdInfo buildJdInfo(Jd jd) {
    List<String> hardSkills = parseSkillTags(jd.getHardSkill());
    List<String> softSkills = parseSkillTags(jd.getSoftSkill());

    String startDate = jd.getStartDate() != null ? jd.getStartDate().format(DATE_FORMATTER) : null;
    String endDate = jd.getEndDate() != null ? jd.getEndDate().format(DATE_FORMATTER) : null;

    return new JdInfo(
        jd.getCompanyName(),
        jd.getRecruitmentField(),
        startDate,
        endDate,
        hardSkills,
        softSkills,
        jd.getMainResponsibilities(),
        jd.getRequiredQualifications(),
        jd.getPreferredQualifications());
  }

  private List<String> parseSkillTags(String skillString) {
    if (skillString == null || skillString.isBlank()) return Collections.emptyList();
    return Arrays.stream(skillString.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .toList();
  }
}
