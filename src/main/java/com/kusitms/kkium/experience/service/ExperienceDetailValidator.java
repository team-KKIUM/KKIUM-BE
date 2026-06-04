package com.kusitms.kkium.experience.service;

import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_COMPANY_TOO_LONG;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_EDUCATION_NAME_TOO_LONG;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_ORGANIZATION_NAME_TOO_LONG;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXPERIENCE_ROLE_TOO_LONG;
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.INVALID_INPUT_VALUE;

import org.springframework.stereotype.Component;

import com.kusitms.kkium.experience.domain.type.PieceType;
import com.kusitms.kkium.experience.dto.request.ExperienceUpdateRequest.Detail;
import com.kusitms.kkium.global.exception.BaseException;

/** 경험 유형별 상세 필드 유효성 검증을 담당한다. */
@Component
public class ExperienceDetailValidator {

  private static final int MAX_ROLE_LENGTH = 50;
  private static final int MAX_COMPANY_LENGTH = 50;
  private static final int MAX_ORGANIZATION_NAME_LENGTH = 50;
  private static final int MAX_EDUCATION_NAME_LENGTH = 80;

  /**
   * PieceType에 맞는 유효성 검증을 실행한다.
   *
   * @throws BaseException INVALID_INPUT_VALUE — 필수 필드 누락
   * @throws BaseException EXPERIENCE_ROLE_TOO_LONG — role 50자 초과
   * @throws BaseException EXPERIENCE_COMPANY_TOO_LONG — company 50자 초과
   * @throws BaseException EXPERIENCE_ORGANIZATION_NAME_TOO_LONG — organizationName 50자 초과
   * @throws BaseException EXPERIENCE_EDUCATION_NAME_TOO_LONG — name 80자 초과
   */
  public void validate(PieceType type, Detail detail) {
    if (type != PieceType.ETC && detail == null) {
      throw new BaseException(INVALID_INPUT_VALUE);
    }
    switch (type) {
      case ACTIVITY -> validateActivity(detail);
      case CAREER -> validateCareer(detail);
      case EDUCATION -> validateEducation(detail);
      case ETC -> {} // ETC는 필수 필드 없음
    }
  }

  private void validateActivity(Detail detail) {
    if (detail.name() == null
        || detail.teamNum() == null
        || detail.role() == null
        || detail.contributionRate() == null) {
      throw new BaseException(INVALID_INPUT_VALUE);
    }
    if (detail.role().length() > MAX_ROLE_LENGTH) {
      throw new BaseException(EXPERIENCE_ROLE_TOO_LONG);
    }
  }

  private void validateCareer(Detail detail) {
    if (detail.company() == null || detail.employmentStatus() == null) {
      throw new BaseException(INVALID_INPUT_VALUE);
    }
    if (detail.company().length() > MAX_COMPANY_LENGTH) {
      throw new BaseException(EXPERIENCE_COMPANY_TOO_LONG);
    }
  }

  private void validateEducation(Detail detail) {
    if (detail.organizationName() == null || detail.name() == null) {
      throw new BaseException(INVALID_INPUT_VALUE);
    }
    if (detail.organizationName().length() > MAX_ORGANIZATION_NAME_LENGTH) {
      throw new BaseException(EXPERIENCE_ORGANIZATION_NAME_TOO_LONG);
    }
    if (detail.name().length() > MAX_EDUCATION_NAME_LENGTH) {
      throw new BaseException(EXPERIENCE_EDUCATION_NAME_TOO_LONG);
    }
  }
}
