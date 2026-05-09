package com.kusitms.kkium.global.response;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonPropertyOrder({"status", "code", "message", "data"})
public class ApiResponse<T> {

  private int status;
  private String code;
  private String message;
  private T data;

  // 성공 응답 - 데이터 있음
  public static <T> ApiResponse<T> success(T data) {
    ApiResponse<T> response = new ApiResponse<>();
    response.status = HttpStatus.OK.value();
    response.code = "SUCCESS";
    response.message = "요청이 성공했습니다.";
    response.data = data;
    return response;
  }

  // 성공 응답 - 커스텀 메시지
  public static <T> ApiResponse<T> success(String message, T data) {
    ApiResponse<T> response = new ApiResponse<>();
    response.status = HttpStatus.OK.value();
    response.code = "SUCCESS";
    response.message = message;
    response.data = data;
    return response;
  }

  // 성공 응답 - 데이터 없음 (삭제, 수정 등)
  public static <T> ApiResponse<T> successWithNoContent() {
    ApiResponse<T> response = new ApiResponse<>();
    response.status = HttpStatus.OK.value();
    response.code = "SUCCESS";
    response.message = "요청이 성공했습니다.";
    response.data = null;
    return response;
  }

  public static <T> ApiResponse<T> successWithNoContent(String message) {
    ApiResponse<T> response = new ApiResponse<>();
    response.status = HttpStatus.OK.value();
    response.code = "SUCCESS";
    response.message = message;
    response.data = null;
    return response;
  }

  // 실패 응답
  public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
    ApiResponse<T> response = new ApiResponse<>();
    response.status = errorCode.getStatus().value();
    response.code = errorCode.getCode();
    response.message = errorCode.getMessage();
    response.data = null;
    return response;
  }

  // 실패 응답 - 커스텀 메시지
  public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
    ApiResponse<T> response = new ApiResponse<>();
    response.status = errorCode.getStatus().value();
    response.code = errorCode.getCode();
    response.message = message;
    response.data = null;
    return response;
  }
}
