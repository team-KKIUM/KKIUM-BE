package com.kusitms.kkium.jd.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JdTitleUpdateRequest(
    @NotBlank(message = "제목은 비어있을 수 없습니다.") @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title) {}
