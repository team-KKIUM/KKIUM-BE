package com.kusitms.kkium.jd.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JdQuestionCreateRequest(
    @NotBlank(message = "자소서 문항은 비어있을 수 없습니다.") @Size(max = 300, message = "자소서 문항은 300자 이하여야 합니다.")
        String content) {}
