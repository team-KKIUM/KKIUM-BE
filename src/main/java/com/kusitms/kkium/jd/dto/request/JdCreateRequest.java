package com.kusitms.kkium.jd.dto.request;

import jakarta.validation.constraints.NotBlank;

public record JdCreateRequest(@NotBlank String linkUrl) {}
