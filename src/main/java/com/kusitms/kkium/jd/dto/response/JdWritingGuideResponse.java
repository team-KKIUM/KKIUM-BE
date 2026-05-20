package com.kusitms.kkium.jd.dto.response;

import java.util.List;

public record JdWritingGuideResponse(
    List<String> coreKeywords, String connectionToJd, String writingGuide) {}
