package com.employment.global.common;

import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

public class Problem extends RuntimeException {

    public final int status;

    public Problem(int status, String message) {
        super(message);
        this.status = status;
    }

    @RestControllerAdvice
    public static class Handler {

        @ExceptionHandler(Problem.class)
        ResponseEntity<?> handle(Problem ex) {
            return response(ex.status, ex.getMessage());
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        ResponseEntity<?> conflict() {
            return response(409, "중복 정보 또는 참조 중인 데이터입니다.");
        }

        @ExceptionHandler(
            {
                IllegalArgumentException.class,
                org.springframework.http.converter.HttpMessageNotReadableException.class,
                org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            }
        )
        ResponseEntity<?> invalid() {
            return response(400, "입력값을 확인해 주세요.");
        }

        @ExceptionHandler(Exception.class)
        ResponseEntity<?> unexpected(Exception ex) {
            return response(500, "서버 처리 중 오류가 발생했습니다.");
        }

        private ResponseEntity<?> response(int status, String message) {
            return ResponseEntity.status(status).body(
                Map.of(
                    "code",
                    "HTTP_" + status,
                    "message",
                    message,
                    "fieldErrors",
                    java.util.List.of(),
                    "traceId",
                    UUID.randomUUID().toString()
                )
            );
        }
    }
}
