package com.book.support.web;

import com.book.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> handleBusiness(final BusinessException exception) {
        final var error = exception.errorCode();
        final HttpStatus status =
                switch (error.category()) {
                    case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
                    case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
                    case NOT_FOUND -> HttpStatus.NOT_FOUND;
                    case CONFLICT -> HttpStatus.CONFLICT;
                    case EXTERNAL_SERVICE_ERROR -> HttpStatus.BAD_GATEWAY;
                    case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
                };
        return ResponseEntity.status(status).body(new ErrorResponse(error.code(), error.message()));
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<ErrorResponse> handleInvalidRequest(final Exception exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse("INVALID_REQUEST", "요청 형식이 올바르지 않습니다."));
    }
}
