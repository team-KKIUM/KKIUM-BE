package com.kusitms.kkium.experience.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_NOT_FOUND;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.kusitms.kkium.experience.domain.*;
import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.experience.dto.request.ExperienceCreateRequest;
import com.kusitms.kkium.experience.dto.request.TagCreateRequest;
import com.kusitms.kkium.experience.dto.response.ExperienceCardResponse;
import com.kusitms.kkium.experience.dto.response.ExperienceListResponse;
import com.kusitms.kkium.experience.dto.response.TagResponse;
import com.kusitms.kkium.experience.repository.*;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExperienceService {

  private static final int DEFAULT_PAGE_SIZE = 10;

  private final UserRepository userRepository;
  private final PieceRepository pieceRepository;
  private final ExperienceRepository experienceRepository;
  private final ActivityRepository activityRepository;
  private final CareerRepository careerRepository;
  private final EducationRepository educationRepository;
  private final EtcRepository etcRepository;
  private final TagRepository tagRepository;
  private final ExperienceEmbeddingService experienceEmbeddingService;

  @Transactional(readOnly = true)
  public ExperienceListResponse getList(Long userId, PieceType type, Long cursor, int size) {
    Pageable pageable = PageRequest.of(0, size + 1);

    List<Experience> experiences;
    if (type == null) {
      experiences =
          cursor == null
              ? experienceRepository.findAllByUserId(userId, pageable)
              : experienceRepository.findAllByUserIdAndCursor(userId, cursor, pageable);
    } else {
      experiences =
          cursor == null
              ? experienceRepository.findAllByUserIdAndType(userId, type, pageable)
              : experienceRepository.findAllByUserIdAndTypeAndCursor(
                  userId, type, cursor, pageable);
    }

    boolean hasNext = experiences.size() > size;
    List<Experience> content = hasNext ? experiences.subList(0, size) : experiences;

    List<Long> experienceIds = content.stream().map(Experience::getId).toList();

    // 태그 벌크 조회
    Map<Long, List<TagResponse>> tagMap =
        tagRepository.findByExperienceIdIn(experienceIds).stream()
            .collect(
                Collectors.groupingBy(
                    t -> t.getExperience().getId(),
                    Collectors.mapping(
                        t -> new TagResponse(t.getCategory(), t.getField()), Collectors.toList())));

    // 기간 벌크 조회
    Map<Long, LocalDate[]> periodMap = resolvePeriodBulk(content, experienceIds);

    List<ExperienceCardResponse> cards =
        content.stream()
            .map(
                e -> {
                  LocalDate[] period =
                      periodMap.getOrDefault(e.getId(), new LocalDate[] {null, null});
                  return new ExperienceCardResponse(
                      e.getPiece().getId(),
                      e.getId(),
                      e.getPiece().getType(),
                      e.getTitle(),
                      e.getOneLineIntro(),
                      period[0],
                      period[1],
                      tagMap.getOrDefault(e.getId(), List.of()));
                })
            .toList();

    Long nextCursor = hasNext ? content.get(content.size() - 1).getId() : null;
    return new ExperienceListResponse(hasNext, nextCursor, cards);
  }

  private Map<Long, LocalDate[]> resolvePeriodBulk(
      List<Experience> content, List<Long> experienceIds) {
    Map<Long, LocalDate[]> periodMap = new HashMap<>();

    Map<PieceType, List<Long>> byType =
        content.stream()
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

  @Transactional
  public void save(Long userId, ExperienceCreateRequest request) {
    User user = findUser(userId);
    Piece piece = pieceRepository.save(Piece.builder().type(request.type()).user(user).build());
    Experience experience =
        experienceRepository.save(
            Experience.builder()
                .title(request.title())
                .oneLineIntro(request.oneLineIntro())
                .situation(request.situation())
                .task(request.task())
                .act(request.act())
                .result(request.result())
                .taken(request.taken())
                .piece(piece)
                .build());

    switch (request.type()) {
      case ACTIVITY ->
          activityRepository.save(
              Activity.builder()
                  .name(request.name())
                  .teamNum(request.teamNum())
                  .startDate(request.startDate())
                  .endDate(request.endDate())
                  .contributionRate(request.contributionRate())
                  .role(request.role())
                  .experience(experience)
                  .build());
      case CAREER ->
          careerRepository.save(
              Career.builder()
                  .name(request.title())
                  .company(request.company())
                  .employmentStatus(request.employmentStatus())
                  .startDate(request.startDate())
                  .endDate(request.endDate())
                  .experience(experience)
                  .build());
      case EDUCATION ->
          educationRepository.save(
              Education.builder()
                  .organizationName(request.organizationName())
                  .name(request.name())
                  .startDate(request.startDate())
                  .endDate(request.endDate())
                  .experience(experience)
                  .build());
      case ETC ->
          etcRepository.save(
              Etc.builder()
                  .startDate(request.startDate())
                  .endDate(request.endDate())
                  .experience(experience)
                  .build());
    }

    saveTags(request.tags(), experience);
    Long pieceId = piece.getId();
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            experienceEmbeddingService.embedPiece(
                pieceId,
                request.title(),
                request.situation(),
                request.task(),
                request.act(),
                request.result(),
                request.taken());
          }
        });
  }

  private User findUser(Long userId) {
    return userRepository.findById(userId).orElseThrow(() -> new BaseException(USER_NOT_FOUND));
  }

  private void saveTags(List<TagCreateRequest> tags, Experience experience) {
    List<Tag> tagEntities =
        tags.stream()
            .map(
                t ->
                    Tag.builder()
                        .category(t.category())
                        .field(t.field())
                        .experience(experience)
                        .build())
            .toList();
    tagRepository.saveAll(tagEntities);
  }
}
