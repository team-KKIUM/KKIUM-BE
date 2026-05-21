package com.kusitms.kkium.resume.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record AiDraftRequest(@NotEmpty @Size(min = 1, max = 3) List<Long> experienceIds) {}
