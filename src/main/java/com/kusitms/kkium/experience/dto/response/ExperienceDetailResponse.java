package com.kusitms.kkium.experience.dto.response;

import java.util.List;

import com.kusitms.kkium.experience.domain.type.PieceType;

public record ExperienceDetailResponse(
    Long pieceId,
    Long experienceId,
    PieceType type,
    String title,
    String oneLineIntro,
    List<TagResponse> tags,
    String situation,
    String task,
    String act,
    String result,
    String taken,
    Object detail) {}
