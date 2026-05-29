package com.kusitms.kkium.experience.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ExperienceTitleUpdateRequest(
    @NotBlank(message = "제목을 입력해주세요.") @Size(max = 80, message = "제목은 80자 이하여야 합니다.")
        String title) {}
