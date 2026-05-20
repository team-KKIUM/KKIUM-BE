package com.kusitms.kkium.resume.dto.response;

import java.util.List;

public record ResumeWritingGuideResponse(
    List<String> coreKeywords, String connectionToJd, String writingGuide) {}
