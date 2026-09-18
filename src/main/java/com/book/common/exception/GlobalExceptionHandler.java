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
    @ExceptionHandler(CoreException.class)
    ResponseEntity<ErrorResponse> handleCoreException(final CoreException exception) {
        final var error = exception.errorType();
        return ResponseEntity.status(error.status()).body(ErrorResponse.of(error, exception.data()));
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class,
        ConstraintViolationException.class
    })
    ResponseEntity<ErrorResponse> handleInvalidRequest(final Exception exception) {
        final var error = ErrorType.INVALID_REQUEST;
        return ResponseEntity.status(error.status()).body(ErrorResponse.of(error));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleUnexpected(final Exception exception) {
        final var error = ErrorType.DEFAULT_ERROR;
        return ResponseEntity.status(error.status()).body(ErrorResponse.of(error));
    }
}
