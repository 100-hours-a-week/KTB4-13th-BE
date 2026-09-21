package com.book.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// NOTE: ThreadLocal을 사용하여 traceId를 관리하는 Filter -> Scoped Value를 사용하여 traceId를 관리하는 Filter로 변경
@Component
public class TraceIdFilter extends OncePerRequestFilter {
    public static final String TRACE_ID_MDC_KEY = "traceId";
    private static final String TRACE_ID_HEADER_KEY = "X-Trace-Id";

    @Override
    protected void doFilterInternal(
            final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {
        final String traceId = UUID.randomUUID().toString();
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER_KEY, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }
}
