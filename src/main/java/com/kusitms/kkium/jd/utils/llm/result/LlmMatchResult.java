package com.kusitms.kkium.jd.utils.llm.result;

import java.util.Map;

public record LlmMatchResult(Map<Long, Integer> usageScores, int applicationScore) {}
