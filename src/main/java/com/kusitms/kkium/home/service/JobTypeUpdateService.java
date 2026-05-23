package com.kusitms.kkium.home.service;

import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kusitms.kkium.home.service.llm.JobTypeLlmService;
import com.kusitms.kkium.user.domain.User;
import com.kusitms.kkium.user.domain.type.JobType;
import com.kusitms.kkium.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobTypeUpdateService {

  private final UserRepository userRepository;
  private final JobTypeLlmService jobTypeLlmService;

  @Async("jobTypeExecutor")
  @Transactional
  public void updateJobType(Long userId) {
    try {
      User user = userRepository.findById(userId).orElseThrow();
      JobType jobType = jobTypeLlmService.analyzeJobType(userId);
      user.updateJobType(jobType);
      log.info("[직무유형] userId={} → {}", userId, jobType);
    } catch (Exception e) {
      log.error("[직무유형] 업데이트 실패 userId={}: {}", userId, e.getMessage());
    }
  }
}
