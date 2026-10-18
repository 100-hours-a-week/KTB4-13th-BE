package com.book.common.exception;

import com.book.common.response.ErrorResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(CoreException.class)
    ResponseEntity<ErrorResponse> handleCoreException(final CoreException exception) {
        final ErrorCode errorCode = exception.errorCode();
        return ResponseEntity.status(errorCode.status()).body(ErrorResponse.of(errorCode, exception.data()));
    }

    @ExceptionHandler(RetryAfterException.class)
    ResponseEntity<ErrorResponse> handleRetryAfter(final RetryAfterException exception) {
        final ErrorCode errorCode = exception.errorCode();
        return ResponseEntity.status(errorCode.status()).header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.retryAfterSeconds()))
            .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class, ConstraintViolationException.class})
    ResponseEntity<ErrorResponse> handleInvalidRequest(final Exception exception) {
        final ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        return ResponseEntity.status(errorCode.status()).body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, OptimisticLockException.class})
    ResponseEntity<ErrorResponse> handleConcurrentChange(final Exception exception) {
        return ResponseEntity.status(ErrorCode.CONCURRENT_CHANGE.status()).body(ErrorResponse.of(ErrorCode.CONCURRENT_CHANGE));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintConflict(final DataIntegrityViolationException exception) {
        // 같은 키·집합의 유일 제약 경합만 충돌로 공개하며 다른 무결성 오류는 내부 오류로 남깁니다.
        final Set<String> concurrentKeys =
            Set.of("uk_cart_item_cart_product", "uk_cart_proposal_source", "uk_cart_change_request_key", "uk_cart_execution_request",
                "uk_cart_execution_proposal", "uk_chat_request_key", "uk_chat_message_sequence", "uk_chat_message_request_role");
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof final org.hibernate.exception.ConstraintViolationException violation) {
                final String name = violation.getConstraintName();
                if (name != null && concurrentKeys.stream().anyMatch((final String key) -> name.equals(key) || name.endsWith("." + key))) {
                    return handleConcurrentChange(exception);
                }
            }
            cause = cause.getCause();
        }
        return handleUnexpected(exception);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(final Exception exception) {
        final ErrorCode errorCode = ErrorCode.DEFAULT_ERROR;
        return ResponseEntity.status(errorCode.status()).body(ErrorResponse.of(errorCode));
    }
}
