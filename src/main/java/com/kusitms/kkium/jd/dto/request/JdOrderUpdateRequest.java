package com.kusitms.kkium.jd.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record JdOrderUpdateRequest(@NotEmpty(message = "jdIds는 비어있을 수 없습니다.") List<Long> jdIds) {}
