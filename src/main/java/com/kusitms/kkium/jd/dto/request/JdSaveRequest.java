package com.kusitms.kkium.jd.dto.request;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record JdSaveRequest(
    String url,
    String postingTitle,
    String companyName,
    String recruitmentField,
    LocalDateTime startDate,
    LocalDateTime endDate,
    @NotEmpty List<@NotBlank @Size(max = 300, message = "자소서 문항은 300자 이하여야 합니다.") String> questions,
    @Size(max = 10000, message = "공고 본문은 10000자 이하여야 합니다.") String content) {}
