package com.kusitms.kkium.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record BasicLoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
