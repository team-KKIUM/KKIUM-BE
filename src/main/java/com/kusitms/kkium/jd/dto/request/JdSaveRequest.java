package com.kusitms.kkium.jd.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

public record JdSaveRequest(
    @NotBlank String url,
    String postingTitle,
    String companyName,
    String recruitmentField,
    String startDate,
    String endDate,
    List<String> questions,
    String content) {}
