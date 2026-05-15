package com.kusitms.kkium.experience.dto.response;

import java.util.List;

import com.kusitms.kkium.experience.domain.Experience;
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
    Object detail) {

  public static ExperienceDetailResponse of(
      Experience experience, List<TagResponse> tags, Object detail) {
    return new ExperienceDetailResponse(
        experience.getPiece().getId(),
        experience.getId(),
        experience.getPiece().getType(),
        experience.getTitle(),
        experience.getOneLineIntro(),
        tags,
        experience.getSituation(),
        experience.getTask(),
        experience.getAct(),
        experience.getResult(),
        experience.getTaken(),
        detail);
  }
}
