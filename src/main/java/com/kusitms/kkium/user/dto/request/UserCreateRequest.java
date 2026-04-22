package com.kusitms.kkium.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UserCreateRequest(@NotBlank String name){}