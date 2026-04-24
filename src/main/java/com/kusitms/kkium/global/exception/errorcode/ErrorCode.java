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

  // user
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "존재하지 않은 유저입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  ErrorCode(HttpStatus status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }
}
