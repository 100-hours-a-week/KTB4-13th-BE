package com.book.common.response;

import com.book.common.exception.ErrorCode;
import com.book.common.logging.TraceIdFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(boolean success, String code, String message, String traceId, Object data) {
    public static ErrorResponse of(final ErrorCode errorCode) {
        return of(errorCode, null);
    }

    public static ErrorResponse of(final ErrorCode errorCode, final Object data) {
        return new ErrorResponse(
                false, errorCode.code(), errorCode.message(), MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY), data);
    }
}
