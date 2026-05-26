package com.kusitms.kkium.experience.dto.response;

import java.util.List;

public record ExperienceListResponse(
    boolean hasNext, Integer nextCursor, List<ExperienceCardResponse> experiences) {}
