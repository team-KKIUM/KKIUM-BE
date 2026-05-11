package com.kusitms.kkium.jd.dto.response;

import java.util.List;

public record JdListPageResponse(
    List<JdListResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext) {}
