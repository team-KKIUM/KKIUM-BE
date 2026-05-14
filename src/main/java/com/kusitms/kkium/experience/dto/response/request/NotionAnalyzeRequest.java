package com.kusitms.kkium.experience.dto.response.request;

import jakarta.validation.constraints.NotBlank;

public record NotionAnalyzeRequest(@NotBlank String pageId) {}
