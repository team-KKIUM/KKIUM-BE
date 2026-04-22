package com.kusitms.puzzl.global.response;

import com.kusitms.puzzl.global.exception.errorcode.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApiResponse<T> {

    private Boolean isSuccess;
    private String code;
    private String message;
    private T data;

    // 성공 응답 - 데이터 있음
    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.isSuccess = true;
        response.code = "SUCCESS";
        response.message = "요청이 성공했습니다.";
        response.data = data;
        return response;
    }

    // 성공 응답 - 커스텀 메시지
    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.isSuccess = true;
        response.code = "SUCCESS";
        response.message = message;
        response.data = data;
        return response;
    }

    // 성공 응답 - 데이터 없음 (삭제, 수정 등)
    public static <T> ApiResponse<T> successWithNoContent() {
        ApiResponse<T> response = new ApiResponse<>();
        response.isSuccess = true;
        response.code = "SUCCESS";
        response.message = "요청이 성공했습니다.";
        response.data = null;
        return response;
    }

    public static <T> ApiResponse<T> successWithNoContent(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.isSuccess = true;
        response.code = "SUCCESS";
        response.message = message;
        response.data = null;
        return response;
    }


    // 실패 응답
    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        ApiResponse<T> response = new ApiResponse<>();
        response.isSuccess = false;
        response.code = errorCode.getCode();
        response.message = errorCode.getMessage();
        response.data = null;
        return response;
    }

    // 실패 응답 - 커스텀 메시지
    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.isSuccess = false;
        response.code = errorCode.getCode();
        response.message = message;
        response.data = null;
        return response;
    }
}