package com.kusitms.kkium.jd.utils.llm.result;

import java.util.Map;

public record LlmQuestionMatchResult(Map<Long, Integer> usageScores) {}
