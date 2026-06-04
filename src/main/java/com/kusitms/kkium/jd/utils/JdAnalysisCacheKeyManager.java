package com.kusitms.kkium.jd.utils;

/** JD 경험 상세 분석 Redis 캐시 키 생성을 중앙 관리한다. */
public final class JdAnalysisCacheKeyManager {

  private static final String PREFIX = "jd-analysis:";

  private JdAnalysisCacheKeyManager() {}

  /** 단건 캐시 키: jd-analysis:{experienceId}:{jdId} */
  public static String of(Long experienceId, Long jdId) {
    return PREFIX + experienceId + ":" + jdId;
  }

  /** 경험 기준 와일드카드: jd-analysis:{experienceId}:* (경험 수정/삭제 시 무효화) */
  public static String byExperience(Long experienceId) {
    return PREFIX + experienceId + ":*";
  }

  /** JD 기준 와일드카드: jd-analysis:*:{jdId} (JD 삭제 시 무효화) */
  public static String byJd(Long jdId) {
    return PREFIX + "*:" + jdId;
  }
}
