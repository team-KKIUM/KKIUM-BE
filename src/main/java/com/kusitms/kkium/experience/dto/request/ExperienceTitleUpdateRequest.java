package com.kusitms.kkium.experience.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ExperienceTitleUpdateRequest(@NotBlank(message = "제목을 입력해주세요.") String title) {}
