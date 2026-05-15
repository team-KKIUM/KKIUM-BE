package com.kusitms.kkium.experience.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.kusitms.kkium.experience.domain.type.PieceType;

public record ExperienceCreateRequest(
    @NotNull PieceType type,
    @NotBlank String title,
    @NotBlank String oneLineIntro,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotBlank String situation,
    @NotBlank String task,
    @NotBlank String act,
    @NotBlank String result,
    @NotBlank String taken,
    @NotNull @Valid List<TagCreateRequest> tags,

    // ACTIVITY, EDUCATION 공통
    String name,

    // ACTIVITY 전용
    Integer teamNum,
    String role,
    Integer contributionRate,

    // CAREER 전용
    String company,
    String employmentStatus,

    // EDUCATION 전용
    String organizationName) {}
