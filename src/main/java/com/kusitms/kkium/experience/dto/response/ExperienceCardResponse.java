package com.kusitms.kkium.experience.dto.response;

import java.time.LocalDate;
import java.util.List;

import com.kusitms.kkium.experience.domain.type.PieceType;

public record ExperienceCardResponse(
    Long pieceId,
    Long experienceId,
    PieceType type,
    String title,
    String oneLineIntro,
    LocalDate startDate,
    LocalDate endDate,
    List<TagResponse> tags) {}
