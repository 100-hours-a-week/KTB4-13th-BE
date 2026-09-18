package com.book.common.exception;

import com.book.common.response.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> handleBusiness(final BusinessException exception) {
        final var error = exception.errorCode();
        return ResponseEntity.status(error.statusCode()).body(ErrorResponse.of(error));
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class,
        ConstraintViolationException.class
    })
    ResponseEntity<ErrorResponse> handleInvalidRequest(final Exception exception) {
        final var error = CommonErrorCode.INVALID_REQUEST;
        return ResponseEntity.badRequest().body(ErrorResponse.of(error));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(final Exception exception) {
        final var error = CommonErrorCode.INTERNAL_ERROR;
        return ResponseEntity.internalServerError().body(ErrorResponse.of(error));
    }
}
