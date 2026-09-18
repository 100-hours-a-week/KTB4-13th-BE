package com.book.common.response;

import com.book.common.exception.ErrorCode;
import com.book.common.logging.TraceIdFilter;
import org.slf4j.MDC;

public record ErrorResponse(boolean success, String code, String message, String traceId) {
    public static ErrorResponse of(final ErrorCode errorCode) {
        return of(errorCode.code(), errorCode.message());
    }

    public static ErrorResponse of(final String code, final String message) {
        return new ErrorResponse(false, code, message, MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY));
    }
}
