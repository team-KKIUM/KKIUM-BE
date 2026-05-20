package com.kusitms.kkium.experience.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.kusitms.kkium.experience.domain.type.PieceType;

public record ExperienceOrderUpdateRequest(
    @NotNull(message = "type은 필수입니다.") PieceType type,
    @NotEmpty(message = "experienceIds는 비어있을 수 없습니다.") List<Long> experienceIds) {}
