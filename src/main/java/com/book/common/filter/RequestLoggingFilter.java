package com.book.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * API 요청 처리 결과를 기록하며, 요청 정보는 구조화된 로그에 MDC 필드로 전달합니다.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
        throws jakarta.servlet.ServletException, java.io.IOException {
        final long startTime = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            final long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            MDC.put("durationMs", Long.toString(durationMs));
            MDC.put("responseStatus", String.valueOf(response.getStatus()));
            try {
                LOGGER.info("[HTTP_REQUEST] 요청 처리가 완료되었습니다. method={} uri={} status={} durationMs={}", request.getMethod(),
                    request.getRequestURI(), response.getStatus(), durationMs);
            } finally {
                MDC.remove("durationMs");
                MDC.remove("responseStatus");
            }
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        final String uri = request.getRequestURI();
        return uri.startsWith("/actuator") || uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs");
    }
}
