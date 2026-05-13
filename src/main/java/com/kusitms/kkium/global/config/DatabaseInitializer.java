package com.kusitms.kkium.global.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements ApplicationRunner {

  private final JdbcTemplate jdbcTemplate;

  @Override
  public void run(ApplicationArguments args) {
    try {
      jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
      jdbcTemplate.execute("ALTER TABLE jds ADD COLUMN IF NOT EXISTS embedding vector(1536)");
      log.info("pgvector 초기화 완료");
    } catch (Exception e) {
      log.warn("pgvector 초기화 실패: {}", e.getMessage());
    }
  }
}
