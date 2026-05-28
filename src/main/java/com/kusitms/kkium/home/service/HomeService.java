package com.kusitms.kkium.home.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;
import com.kusitms.kkium.home.dto.response.HomeResponse;
import com.kusitms.kkium.home.dto.response.HomeResponse.ExperienceDistribution;
import com.kusitms.kkium.home.dto.response.HomeResponse.JobTypeInfo;
import com.kusitms.kkium.home.dto.response.HomeResponse.TargetJdInfo;
import com.kusitms.kkium.home.repository.HomeRepository;
import com.kusitms.kkium.jd.domain.Jd;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

  private final HomeRepository homeRepository;
  private final UserRepository userRepository;

  public HomeResponse getHome(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

    // 목표 공고
    List<TargetJdInfo> targetJdInfos =
        homeRepository.findTargetJds(userId).stream().map(this::buildTargetJdInfo).toList();

    // 전체 경험 수
    int totalCount = homeRepository.countTotalExperience(userId);

    // 이번 달 경험 수
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startOfMonth = now.withDayOfMonth(1).toLocalDate().atStartOfDay();
    LocalDateTime startOfNextMonth = startOfMonth.plusMonths(1);
    int thisMonthCount =
        homeRepository.countThisMonthExperience(userId, startOfMonth, startOfNextMonth);

    // 지난달 경험 수 및 증감
    LocalDateTime startOfLastMonth = startOfMonth.minusMonths(1);
    int lastMonthCount =
        homeRepository.countThisMonthExperience(userId, startOfLastMonth, startOfMonth);
    int lastMonthDiff = thisMonthCount - lastMonthCount;

    // 직무 유형
    JobTypeInfo jobTypeInfo =
        user.getJobType() != null
            ? new JobTypeInfo(user.getJobType().getLabel())
            : new JobTypeInfo(null);

    // 경험 분포
    List<Object[]> rawCounts = homeRepository.countByPieceType(userId, PieceType.ALL);
    Map<PieceType, Integer> countMap =
        rawCounts.stream()
            .collect(Collectors.toMap(r -> (PieceType) r[0], r -> ((Long) r[1]).intValue()));

    List<ExperienceDistribution> distribution =
        countMap.entrySet().stream()
            .map(
                entry -> {
                  int percentage =
                      totalCount > 0 ? Math.round(entry.getValue() * 100f / totalCount) : 0;
                  return new ExperienceDistribution(entry.getKey(), entry.getValue(), percentage);
                })
            .toList();

    return new HomeResponse(
        targetJdInfos, totalCount, thisMonthCount, lastMonthDiff, jobTypeInfo, distribution);
  }

  private TargetJdInfo buildTargetJdInfo(Jd jd) {
    return new TargetJdInfo(
        jd.getId(),
        jd.getCompanyName(),
        jd.getRecruitmentField(),
        jd.getStartDate(),
        jd.getEndDate(),
        parseSkillTags(jd.getHardSkill()),
        parseSkillTags(jd.getSoftSkill()),
        jd.getApplicationFitScore());
  }

  private List<String> parseSkillTags(String skillString) {
    if (skillString == null || skillString.isBlank()) return Collections.emptyList();
    return Arrays.stream(skillString.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .toList();
  }
}
