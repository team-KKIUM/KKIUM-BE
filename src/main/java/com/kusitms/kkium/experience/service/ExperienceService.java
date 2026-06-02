package com.kusitms.kkium.experience.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_COMPANY_TOO_LONG;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_EDUCATION_NAME_TOO_LONG;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_ORDER_NOT_FOUND;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_ORGANIZATION_NAME_TOO_LONG;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_ROLE_TOO_LONG;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.FORBIDDEN;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.INVALID_INPUT_VALUE;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_NOT_FOUND;

import java.time.LocalDate;
import java.util.Comparator;
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
import com.kusitms.kkium.experience.dto.request.ExperienceOrderUpdateRequest;
import com.kusitms.kkium.experience.dto.request.ExperienceUpdateRequest;
import com.kusitms.kkium.experience.dto.request.TagCreateRequest;
import com.kusitms.kkium.experience.dto.response.ExperienceCardResponse;
import com.kusitms.kkium.experience.dto.response.ExperienceDetailResponse;
import com.kusitms.kkium.experience.dto.response.ExperienceListResponse;
import com.kusitms.kkium.experience.dto.response.TagResponse;
import com.kusitms.kkium.experience.dto.response.detail.ActivityDetail;
import com.kusitms.kkium.experience.dto.response.detail.CareerDetail;
import com.kusitms.kkium.experience.dto.response.detail.EducationDetail;
import com.kusitms.kkium.experience.dto.response.detail.EtcDetail;
import com.kusitms.kkium.experience.repository.*;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.home.service.JobTypeUpdateService;
import com.kusitms.kkium.jd.service.JdExperienceAnalysisService;
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
  private final ExperienceOrderRepository experienceOrderRepository;
  private final ActivityRepository activityRepository;
  private final CareerRepository careerRepository;
  private final EducationRepository educationRepository;
  private final EtcRepository etcRepository;
  private final TagRepository tagRepository;
  private final ExperienceEmbeddingService experienceEmbeddingService;
  private final JobTypeUpdateService jobTypeUpdateService;
  private final JdExperienceAnalysisService jdExperienceAnalysisService;
  private final ExperiencePeriodResolver experiencePeriodResolver;

  @Transactional(readOnly = true)
  public ExperienceDetailResponse getDetail(Long userId, Long experienceId) {
    Experience experience =
        experienceRepository
            .findByIdWithPiece(experienceId)
            .orElseThrow(() -> new BaseException(EXPERIENCE_NOT_FOUND));

    if (!experience.getPiece().getUser().getId().equals(userId)) {
      throw new BaseException(FORBIDDEN);
    }

    List<TagResponse> tags =
        tagRepository.findByExperienceIdIn(List.of(experienceId)).stream()
            .map(t -> new TagResponse(t.getCategory(), t.getField()))
            .toList();

    Object detail =
        switch (experience.getPiece().getType()) {
          case ACTIVITY ->
              activityRepository
                  .findByExperienceId(experienceId)
                  .map(ActivityDetail::from)
                  .orElse(null);
          case CAREER ->
              careerRepository
                  .findByExperienceId(experienceId)
                  .map(CareerDetail::from)
                  .orElse(null);
          case EDUCATION ->
              educationRepository
                  .findByExperienceId(experienceId)
                  .map(EducationDetail::from)
                  .orElse(null);
          case ETC ->
              etcRepository.findByExperienceId(experienceId).map(EtcDetail::from).orElse(null);
          default -> null;
        };

    return ExperienceDetailResponse.of(experience, tags, detail);
  }

  @Transactional(readOnly = true)
  public ExperienceListResponse getList(
      Long userId, PieceType type, Integer cursor, int size, String keyword) {
    if (type == PieceType.ALL) type = null;
    Pageable pageable = PageRequest.of(0, size + 1);

    List<Experience> experiences;

    if (keyword != null && !keyword.isBlank()) {
      // 키워드 검색: 2-step (id 추출 → fetch)
      List<Long> ids;
      if (type == null) {
        ids =
            cursor == null
                ? experienceRepository.findIdsByKeyword(userId, keyword, pageable)
                : experienceRepository.findIdsByKeywordAndCursor(userId, keyword, cursor, pageable);
      } else {
        ids =
            cursor == null
                ? experienceRepository.findIdsByKeywordAndType(userId, keyword, type, pageable)
                : experienceRepository.findIdsByKeywordAndTypeAndCursor(
                    userId, keyword, type, cursor, pageable);
      }
      if (ids.isEmpty()) {
        return new ExperienceListResponse(false, null, List.of());
      }
      experiences = experienceRepository.findAllByIdIn(ids);
      // ids가 이미 sort_order ASC로 정렬되어 있으므로 index 기준으로 정렬
      Map<Long, Integer> idIndexMap = new HashMap<>();
      for (int i = 0; i < ids.size(); i++) {
        idIndexMap.put(ids.get(i), i);
      }
      experiences =
          experiences.stream()
              .sorted(
                  Comparator.comparingInt(
                      e -> idIndexMap.getOrDefault(e.getId(), Integer.MAX_VALUE)))
              .collect(Collectors.toList());
    } else if (type == null) {
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
    Map<Long, LocalDate[]> periodMap = experiencePeriodResolver.resolvePeriodBulk(content);

    // sort_order 벌크 조회 (nextCursor 계산용)
    PieceType orderType = type != null ? type : PieceType.ALL;
    Map<Long, Integer> sortOrderMap =
        experienceOrderRepository
            .findAllByUserIdAndPieceTypeAndExperienceIdIn(userId, orderType, experienceIds)
            .stream()
            .collect(
                Collectors.toMap(eo -> eo.getExperience().getId(), ExperienceOrder::getSortOrder));

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

    Integer nextCursor = hasNext ? sortOrderMap.get(content.get(content.size() - 1).getId()) : null;
    return new ExperienceListResponse(hasNext, nextCursor, cards);
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
      default -> throw new BaseException(INVALID_INPUT_VALUE);
    }

    saveTags(request.tags(), experience);
    saveInitialOrders(user, experience, request.type());
    Long pieceId = piece.getId();
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            experienceEmbeddingService.embedPiece(
                pieceId,
                request.title(),
                request.oneLineIntro(),
                request.situation(),
                request.task(),
                request.act(),
                request.result(),
                request.taken(),
                request.name(),
                request.role(),
                request.company(),
                request.employmentStatus(),
                request.organizationName(),
                request.tags());
            jobTypeUpdateService.updateJobType(userId);
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

  private void saveInitialOrders(User user, Experience experience, PieceType type) {
    List<PieceType> targetTypes = List.of(PieceType.ALL, type);
    List<ExperienceOrder> orders =
        targetTypes.stream()
            .map(
                pieceType -> {
                  int nextOrder =
                      experienceOrderRepository.findMaxSortOrderByUserIdAndPieceType(
                              user.getId(), pieceType)
                          + 1;
                  return ExperienceOrder.builder()
                      .sortOrder(nextOrder)
                      .pieceType(pieceType)
                      .experience(experience)
                      .user(user)
                      .build();
                })
            .toList();
    experienceOrderRepository.saveAll(orders);
  }

  @Transactional
  public void updateOrder(ExperienceOrderUpdateRequest request, Long userId) {
    PieceType type = request.type();
    List<Long> experienceIds = request.experienceIds();

    List<ExperienceOrder> orders =
        experienceOrderRepository.findAllByUserIdAndPieceTypeAndExperienceIdIn(
            userId, type, experienceIds);

    if (orders.size() != experienceIds.size()) {
      throw new BaseException(EXPERIENCE_ORDER_NOT_FOUND);
    }

    orders.forEach(
        order -> {
          if (!order.getUser().getId().equals(userId)) {
            throw new BaseException(FORBIDDEN);
          }
        });

    Map<Long, ExperienceOrder> orderMap =
        orders.stream().collect(Collectors.toMap(o -> o.getExperience().getId(), o -> o));

    for (int i = 0; i < experienceIds.size(); i++) {
      ExperienceOrder order = orderMap.get(experienceIds.get(i));
      if (order != null) {
        order.updateSortOrder(i + 1);
      }
    }
  }

  @Transactional
  public void delete(Long userId, Long experienceId) {
    Experience experience =
        experienceRepository
            .findByIdWithPiece(experienceId)
            .orElseThrow(() -> new BaseException(EXPERIENCE_NOT_FOUND));

    if (!experience.getPiece().getUser().getId().equals(userId)) {
      throw new BaseException(FORBIDDEN);
    }

    experience.getPiece().delete();
    experienceOrderRepository.deleteAllByExperienceId(experienceId);

    // 캐시 무효화
    jdExperienceAnalysisService.evictCache(experienceId);

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            jobTypeUpdateService.updateJobType(userId);
          }
        });
  }

  @Transactional
  public void update(Long userId, Long experienceId, ExperienceUpdateRequest request) {
    Experience experience =
        experienceRepository
            .findByIdWithPiece(experienceId)
            .orElseThrow(() -> new BaseException(EXPERIENCE_NOT_FOUND));

    if (!experience.getPiece().getUser().getId().equals(userId)) {
      throw new BaseException(FORBIDDEN);
    }

    // 1. Experience 공통 필드 수정
    experience.update(
        request.title(),
        request.oneLineIntro(),
        request.situation(),
        request.task(),
        request.act(),
        request.result(),
        request.taken());

    // 2. 태그 전체 삭제 후 재삽입
    tagRepository.deleteAll(tagRepository.findByExperienceId(experienceId));
    List<Tag> newTags =
        request.tags().stream()
            .map(
                t ->
                    Tag.builder()
                        .category(t.category())
                        .field(t.field())
                        .experience(experience)
                        .build())
            .collect(Collectors.toList());
    tagRepository.saveAll(newTags);

    // 3. 유형별 detail 수정
    PieceType type = experience.getPiece().getType();
    ExperienceUpdateRequest.Detail detail = request.detail();

    switch (type) {
      case ACTIVITY -> {
        if (detail.name() == null
            || detail.teamNum() == null
            || detail.role() == null
            || detail.contributionRate() == null) {
          throw new BaseException(INVALID_INPUT_VALUE);
        }
        if (detail.role().length() > 50) throw new BaseException(EXPERIENCE_ROLE_TOO_LONG);
        activityRepository
            .findByExperienceId(experienceId)
            .ifPresent(
                a ->
                    a.update(
                        detail.name(),
                        detail.teamNum(),
                        detail.role(),
                        detail.contributionRate(),
                        detail.startDate(),
                        detail.endDate()));
      }
      case CAREER -> {
        if (detail.company() == null || detail.employmentStatus() == null) {
          throw new BaseException(INVALID_INPUT_VALUE);
        }
        if (detail.company().length() > 50) throw new BaseException(EXPERIENCE_COMPANY_TOO_LONG);
        careerRepository
            .findByExperienceId(experienceId)
            .ifPresent(
                c ->
                    c.update(
                        request.title(),
                        detail.company(),
                        detail.employmentStatus(),
                        detail.startDate(),
                        detail.endDate()));
      }
      case EDUCATION -> {
        if (detail.organizationName() == null || detail.name() == null) {
          throw new BaseException(INVALID_INPUT_VALUE);
        }
        if (detail.organizationName().length() > 50)
          throw new BaseException(EXPERIENCE_ORGANIZATION_NAME_TOO_LONG);
        if (detail.name().length() > 80)
          throw new BaseException(EXPERIENCE_EDUCATION_NAME_TOO_LONG);
        educationRepository
            .findByExperienceId(experienceId)
            .ifPresent(
                e ->
                    e.update(
                        detail.organizationName(),
                        detail.name(),
                        detail.startDate(),
                        detail.endDate()));
      }
      case ETC ->
          etcRepository
              .findByExperienceId(experienceId)
              .ifPresent(e -> e.update(detail.startDate(), detail.endDate()));
    }

    // 캐시 무효화
    jdExperienceAnalysisService.evictCache(experienceId);

    Long pieceId = experience.getPiece().getId();
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            experienceEmbeddingService.embedPiece(
                pieceId,
                request.title(),
                request.oneLineIntro(),
                request.situation(),
                request.task(),
                request.act(),
                request.result(),
                request.taken(),
                request.detail().name(),
                request.detail().role(),
                request.detail().company(),
                request.detail().employmentStatus(),
                request.detail().organizationName(),
                request.tags());
            jobTypeUpdateService.updateJobType(userId);
          }
        });
  }

  @Transactional
  public void updateTitle(Long userId, Long experienceId, String title) {
    Experience experience =
        experienceRepository
            .findByIdWithPiece(experienceId)
            .orElseThrow(() -> new BaseException(EXPERIENCE_NOT_FOUND));

    if (!experience.getPiece().getUser().getId().equals(userId)) {
      throw new BaseException(FORBIDDEN);
    }

    experience.updateTitle(title);

    // 캐시 무효화
    jdExperienceAnalysisService.evictCache(experienceId);

    Long pieceId = experience.getPiece().getId();
    PieceType type = experience.getPiece().getType();
    List<TagCreateRequest> tags =
        tagRepository.findByExperienceId(experienceId).stream()
            .map(t -> new TagCreateRequest(t.getCategory(), t.getField()))
            .toList();

    String name = null,
        role = null,
        company = null,
        employmentStatus = null,
        organizationName = null;
    switch (type) {
      case ACTIVITY -> {
        var a = activityRepository.findByExperienceId(experienceId).orElse(null);
        if (a != null) {
          name = a.getName();
          role = a.getRole();
        }
      }
      case CAREER -> {
        var c = careerRepository.findByExperienceId(experienceId).orElse(null);
        if (c != null) {
          c.update(
              title, c.getCompany(), c.getEmploymentStatus(), c.getStartDate(), c.getEndDate());
          company = c.getCompany();
          employmentStatus = c.getEmploymentStatus();
        }
      }
      case EDUCATION -> {
        var e = educationRepository.findByExperienceId(experienceId).orElse(null);
        if (e != null) {
          name = e.getName();
          organizationName = e.getOrganizationName();
        }
      }
      default -> {}
    }

    String finalName = name,
        finalRole = role,
        finalCompany = company,
        finalEmploymentStatus = employmentStatus,
        finalOrganizationName = organizationName;
    Long finalPieceId = pieceId;

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            experienceEmbeddingService.embedPiece(
                finalPieceId,
                title,
                experience.getOneLineIntro(),
                experience.getSituation(),
                experience.getTask(),
                experience.getAct(),
                experience.getResult(),
                experience.getTaken(),
                finalName,
                finalRole,
                finalCompany,
                finalEmploymentStatus,
                finalOrganizationName,
                tags);
          }
        });
  }
}
