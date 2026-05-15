package com.kusitms.kkium.experience.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.kusitms.kkium.experience.domain.type.TagCategory;

public record TagCreateRequest(@NotNull TagCategory category, @NotBlank String field) {}
