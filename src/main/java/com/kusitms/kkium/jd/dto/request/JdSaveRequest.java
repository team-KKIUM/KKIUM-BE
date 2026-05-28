package com.kusitms.kkium.jd.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record JdSaveRequest(
    @NotBlank String url,
    String postingTitle,
    String companyName,
    String recruitmentField,
    LocalDateTime startDate,
    LocalDateTime endDate,
    @NotNull @Size(min = 1) List<String> questions,
    String content) {}
