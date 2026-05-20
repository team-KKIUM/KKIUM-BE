package com.kusitms.kkium.global.exception.errorcode;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {

  // Common
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "올바르지 않은 입력값입니다."),
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "잘못된 HTTP 메서드를 호출했습니다."),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 에러가 발생했습니다."),
  NOT_FOUND(HttpStatus.NOT_FOUND, "C004", "존재하지 않는 리소스입니다."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C005", "인증이 필요합니다."),
  FORBIDDEN(HttpStatus.FORBIDDEN, "C006", "접근 권한이 없습니다."),

  // auth
  USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "A001", "이미 존재하는 이메일입니다."),
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A002", "이메일 또는 비밀번호가 올바르지 않습니다."),
  INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "유효하지 않은 토큰입니다."),
  LOGIN_KAKAO_TOKEN_FAILED(HttpStatus.UNAUTHORIZED, "A004", "카카오 토큰 발급에 실패했습니다."),
  LOGIN_KAKAO_USERINFO_FAILED(HttpStatus.UNAUTHORIZED, "A005", "카카오 사용자 정보 조회에 실패했습니다."),

  // user
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "존재하지 않은 유저입니다."),

  // jd
  JD_NOT_FOUND(HttpStatus.NOT_FOUND, "J001", "존재하지 않는 공고입니다."),
  JD_TARGET_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "J003", "목표 공고는 최대 5개까지 등록 가능합니다."),
  JD_SCRAPE_FAILED(HttpStatus.BAD_GATEWAY, "J002", "채용공고 내용을 가져오는데 실패했습니다."),
  QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "J004", "존재하지 않는 문항입니다."),
  EXPERIENCE_SELECTION_LIMIT(HttpStatus.BAD_REQUEST, "J005", "경험은 최소 1개 이상, 최대 3개까지 선택 가능합니다."),

  // notion
  NOTION_TOKEN_FAILED(HttpStatus.UNAUTHORIZED, "N001", "Notion 토큰 발급에 실패했습니다."),
  NOTION_NOT_CONNECTED(HttpStatus.UNAUTHORIZED, "N002", "Notion 연결이 필요합니다."),
  NOTION_INVALID_STATE(HttpStatus.BAD_REQUEST, "N003", "유효하지 않은 Notion state 값입니다."),
  NOTION_RESPONSE_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "N004", "Notion 응답 파싱에 실패했습니다."),
  NOTION_BLOCK_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "N005", "Notion 블록 콘텐츠 파싱에 실패했습니다."),

  // experience
  EXPERIENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "E005", "존재하지 않는 경험입니다."),
  EXPERIENCE_ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "E006", "경험 순서 정보가 존재하지 않습니다."),
  INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "E001", "PDF 파일만 업로드 가능합니다."),
  FILE_ALREADY_UPLOADED(HttpStatus.CONFLICT, "E002", "이미 업로드된 자료가 있습니다. 다시 업로드하면 초기화됩니다."),
  PDF_PARSE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E003", "PDF 파일 파싱에 실패했습니다."),
  LLM_CALL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "E004", "AI 분석에 실패했습니다. 다시 시도해주세요.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  ErrorCode(HttpStatus status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }
}
