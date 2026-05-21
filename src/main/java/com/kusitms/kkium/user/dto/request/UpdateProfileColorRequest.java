package com.kusitms.kkium.user.dto.request;

import jakarta.validation.constraints.NotNull;

public record UpdateProfileColorRequest(
    @NotNull(message = "일러스트 ID를 입력해주세요.") Integer illustrateId) {}
