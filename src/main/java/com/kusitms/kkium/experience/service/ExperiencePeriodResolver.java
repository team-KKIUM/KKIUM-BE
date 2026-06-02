package com.kusitms.kkium.experience.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.kusitms.kkium.experience.domain.Experience;
import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.experience.repository.ActivityRepository;
import com.kusitms.kkium.experience.repository.CareerRepository;
import com.kusitms.kkium.experience.repository.EducationRepository;
import com.kusitms.kkium.experience.repository.EtcRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ExperiencePeriodResolver {

  private final ActivityRepository activityRepository;
  private final CareerRepository careerRepository;
  private final EducationRepository educationRepository;
  private final EtcRepository etcRepository;

  public Map<Long, LocalDate[]> resolvePeriodBulk(List<Experience> experiences) {
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
