package com.kusitms.kkium.home.service.llm;

import com.kusitms.kkium.user.domain.type.JobType;

public interface JobTypeLlmService {
  JobType analyzeJobType(Long userId);
}
