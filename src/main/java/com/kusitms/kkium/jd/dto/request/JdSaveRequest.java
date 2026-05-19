package com.kusitms.kkium.jd.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record JdSaveRequest(
    @NotBlank String url,
    String postingTitle,
    String companyName,
    String recruitmentField,
    LocalDateTime startDate,
    LocalDateTime endDate,
    List<String> questions,
    String content) {}
