package com.kusitms.kkium.global.exception.handler;

import com.kusitms.kkium.global.exception.BaseException;
import com.kusitms.kkium.global.exception.errorcode.ErrorCode;
import com.kusitms.kkium.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger("ErrorLogger");
    private static final String LOG_FORMAT_INFO = "[🔵INFO] - ({} {}) {} {}: {}";
    private static final String LOG_FORMAT_ERROR = "[🔴ERROR] - ({} {})";

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<?> handleBaseException(BaseException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn(LOG_FORMAT_INFO, request.getMethod(), request.getRequestURI(),
                errorCode.getStatus().value(), errorCode.getCode(), e.getMessage());
        return toResponse(errorCode);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e, HttpServletRequest request) {
        log.warn(LOG_FORMAT_INFO, request.getMethod(), request.getRequestURI(),
                ErrorCode.INVALID_INPUT_VALUE.getStatus().value(), ErrorCode.INVALID_INPUT_VALUE.getCode(), e.getMessage());
        return toResponse(ErrorCode.INVALID_INPUT_VALUE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllException(Exception e, HttpServletRequest request) {
        // 마지막 인자가 Throwable이면 slf4j가 스택트레이스를 자동으로 붙여줌
        log.error(LOG_FORMAT_ERROR, request.getMethod(), request.getRequestURI(), e);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException e,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        HttpServletRequest req = ((ServletWebRequest) request).getRequest();
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn(LOG_FORMAT_INFO, req.getMethod(), req.getRequestURI(),
                ErrorCode.INVALID_INPUT_VALUE.getStatus().value(), ErrorCode.INVALID_INPUT_VALUE.getCode(), message);
        return ResponseEntity.status(ErrorCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE, message));
    }

    private ResponseEntity<?> toResponse(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode));
    }
}