package com.kusitms.kkium.jd.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record JdSaveRequest(
    @NotBlank String url,
    String postingTitle,
    String companyName,
    String recruitmentField,
    LocalDateTime startDate,
    LocalDateTime endDate,
    @NotEmpty List<@NotBlank String> questions,
    String content) {}
