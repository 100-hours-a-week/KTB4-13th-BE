package com.book.common.response;

import com.book.common.exception.ErrorMessage;
import com.book.common.exception.ErrorType;
import com.book.common.logging.TraceIdFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(boolean success, String code, String message, String traceId, Object data) {
    public static ErrorResponse of(final ErrorType errorType) {
        return of(errorType, null);
    }

    public static ErrorResponse of(final ErrorType errorType, final Object data) {
        final var errorMessage = new ErrorMessage(errorType, data);
        return new ErrorResponse(
                false,
                errorMessage.code(),
                errorMessage.message(),
                MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY),
                errorMessage.data());
    }
}
