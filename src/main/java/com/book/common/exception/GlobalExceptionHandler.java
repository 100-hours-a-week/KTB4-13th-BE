package com.book.common.exception;

import com.book.common.response.ErrorResponse;
import com.book.common.logging.TraceIdFilter;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.slf4j.event.Level;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
@Slf4j
class GlobalExceptionHandler {
    @ExceptionHandler(CoreException.class)
    ResponseEntity<ErrorResponse> handleCoreException(final CoreException exception) {
        final var errorCode = exception.errorCode();
        logBusinessError(errorCode);
        return ResponseEntity.status(errorCode.status()).body(ErrorResponse.of(errorCode, exception.data()));
    }

    @ExceptionHandler(RetryAfterException.class)
    ResponseEntity<ErrorResponse> handleRetryAfter(final RetryAfterException exception) {
        final var errorCode = exception.errorCode();
        logBusinessError(errorCode);
        return ResponseEntity.status(errorCode.status()).header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.retryAfterSeconds()))
            .body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class, ConstraintViolationException.class})
    ResponseEntity<ErrorResponse> handleInvalidRequest(final Exception exception) {
        final var errorCode = ErrorCode.INVALID_REQUEST;
        logBusinessError(errorCode);
        return ResponseEntity.status(errorCode.status()).body(ErrorResponse.of(errorCode));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(final Exception exception) {
        final var errorCode = ErrorCode.DEFAULT_ERROR;
        log.error("요청 처리 중 오류가 발생했습니다. errorCode={}, code={}, traceId={}", errorCode.name(), errorCode.code(),
            MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY), exception);
        return ResponseEntity.status(errorCode.status()).body(ErrorResponse.of(errorCode));
    }

    private void logBusinessError(final ErrorCode errorCode) {
        log.atLevel(Level.valueOf(errorCode.logLevel().name())).log("요청을 처리할 수 없습니다. errorCode={}, code={}, traceId={}", errorCode.name(),
            errorCode.code(), MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY));
    }
}
