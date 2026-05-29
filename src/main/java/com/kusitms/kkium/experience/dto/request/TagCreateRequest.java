package com.kusitms.kkium.experience.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.kusitms.kkium.experience.domain.type.TagCategory;

public record TagCreateRequest(
    @NotNull TagCategory category,
    @NotBlank @Size(max = 15, message = "태그는 15자 이하여야 합니다.") String field) {}
