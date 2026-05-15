package com.kusitms.kkium.experience.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_NOT_FOUND;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.experience.domain.*;
import com.kusitms.kkium.experience.dto.request.ExperienceCreateRequest;
import com.kusitms.kkium.experience.dto.request.TagCreateRequest;
import com.kusitms.kkium.experience.repository.*;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ExperienceService {

  private final UserRepository userRepository;
  private final PieceRepository pieceRepository;
  private final ExperienceRepository experienceRepository;
  private final ActivityRepository activityRepository;
  private final CareerRepository careerRepository;
  private final EducationRepository educationRepository;
  private final EtcRepository etcRepository;
  private final TagRepository tagRepository;
  private final ExperienceEmbeddingService experienceEmbeddingService;

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
    experienceEmbeddingService.embedPiece(
        piece.getId(),
        request.title(),
        request.situation(),
        request.task(),
        request.act(),
        request.result(),
        request.taken());
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
