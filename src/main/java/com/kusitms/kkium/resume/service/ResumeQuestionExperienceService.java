package com.kusitms.kkium.resume.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.JD_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.QUESTION_NOT_FOUND;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.experience.repository.ActivityRepository;
import com.kusitms.kkium.experience.repository.CareerRepository;
import com.kusitms.kkium.experience.repository.EducationRepository;
import com.kusitms.kkium.experience.repository.EtcRepository;
import com.kusitms.kkium.experience.repository.ExperienceRepository;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.jd.domain.JdQuestion;
import com.kusitms.kkium.jd.repository.JdMatchRepository;
import com.kusitms.kkium.jd.repository.JdMatchRepository.PieceSimilarity;
import com.kusitms.kkium.jd.repository.JdQuestionRepository;
import com.kusitms.kkium.jd.repository.JdRepository;
import com.kusitms.kkium.jd.utils.llm.LlmMatchScoreService;
import com.kusitms.kkium.resume.dto.response.ResumeQuestionExperienceResponse;
import com.kusitms.kkium.resume.dto.response.ResumeQuestionExperienceResponse.ExperienceMatchItem;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumeQuestionExperienceService {

  private final JdRepository jdRepository;
  private final JdQuestionRepository jdQuestionRepository;
  private final ExperienceRepository experienceRepository;
  private final ActivityRepository activityRepository;
  private final CareerRepository careerRepository;
  private final EducationRepository educationRepository;
  private final EtcRepository etcRepository;
  private final JdMatchRepository jdMatchRepository;
  private final LlmMatchScoreService llmMatchScoreService;

  public ResumeQuestionExperienceResponse getExperiencesWithFitScore(
      Long jdId, Long questionId, Long userId) {

    // 1. JD 조회
    Jd jd = jdRepository.findById(jdId).orElseThrow(() -> new BaseException(JD_NOT_FOUND));

    // 2. 문항 조회
    JdQuestion question =
        jdQuestionRepository
            .findById(questionId)
            .orElseThrow(() -> new BaseException(QUESTION_NOT_FOUND));

    // 3. 유저의 전체 경험 조회
    List<Experience> allExperiences = experienceRepository.findAllByUserIdNoPage(userId);

    if (allExperiences.isEmpty()) {
      return new ResumeQuestionExperienceResponse(List.of());
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

    // 7. 기간 벌크 조회
    List<Long> experienceIds = allExperiences.stream().map(Experience::getId).toList();
    Map<Long, LocalDate[]> periodMap = resolvePeriodBulk(allExperiences, experienceIds);

    // 8. 응답 구성 (usageFitScore 내림차순 정렬)
    List<ExperienceMatchItem> items =
        allExperiences.stream()
            .map(
                exp -> {
                  LocalDate[] period =
                      periodMap.getOrDefault(exp.getId(), new LocalDate[] {null, null});
                  return new ExperienceMatchItem(
                      exp.getId(),
                      exp.getTitle(),
                      exp.getOneLineIntro(),
                      period[0],
                      period[1],
                      usageFitScoreMap.getOrDefault(exp.getPiece().getId(), 0));
                })
            .sorted(Comparator.comparingInt(ExperienceMatchItem::usageFitScore).reversed())
            .toList();

    return new ResumeQuestionExperienceResponse(items);
  }

  private Map<Long, LocalDate[]> resolvePeriodBulk(
      List<Experience> experiences, List<Long> experienceIds) {
    Map<Long, LocalDate[]> periodMap = new HashMap<>();

    Map<PieceType, List<Long>> byType =
        experiences.stream()
            .collect(
                Collectors.groupingBy(
                    e -> e.getPiece().getType(),
                    Collectors.mapping(Experience::getId, Collectors.toList())));

    if (byType.containsKey(PieceType.ACTIVITY)) {
      activityRepository
          .findByExperienceIdIn(byType.get(PieceType.ACTIVITY))
          .forEach(
              a ->
                  periodMap.put(
                      a.getExperience().getId(),
                      new LocalDate[] {a.getStartDate(), a.getEndDate()}));
    }
    if (byType.containsKey(PieceType.CAREER)) {
      careerRepository
          .findByExperienceIdIn(byType.get(PieceType.CAREER))
          .forEach(
              c ->
                  periodMap.put(
                      c.getExperience().getId(),
                      new LocalDate[] {c.getStartDate(), c.getEndDate()}));
    }
    if (byType.containsKey(PieceType.EDUCATION)) {
      educationRepository
          .findByExperienceIdIn(byType.get(PieceType.EDUCATION))
          .forEach(
              ed ->
                  periodMap.put(
                      ed.getExperience().getId(),
                      new LocalDate[] {ed.getStartDate(), ed.getEndDate()}));
    }
    if (byType.containsKey(PieceType.ETC)) {
      etcRepository
          .findByExperienceIdIn(byType.get(PieceType.ETC))
          .forEach(
              etc ->
                  periodMap.put(
                      etc.getExperience().getId(),
                      new LocalDate[] {etc.getStartDate(), etc.getEndDate()}));
    }

    return periodMap;
  }
}
