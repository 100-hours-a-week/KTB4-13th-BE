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
 * API 요청의 수명주기(응답 시간, 상태 코드)를 로깅하는 필터입니다. RequestMdcFilter 이후에 실행되어 MDC 컨텍스트(requestId)를 활용합니다.
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
            final long duration = (System.nanoTime() - startTime) / 1_000_000;
            MDC.put("requestDuration", Long.toString(duration));
            MDC.put("responseStatus", String.valueOf(response.getStatus()));
            LOGGER.info("HTTP request completed method={} uri={} status={} durationMs={}", request.getMethod(), request.getRequestURI(),
                response.getStatus(), duration);

            MDC.remove("requestDuration");
            MDC.remove("responseStatus");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        final String uri = request.getRequestURI();
        return uri.startsWith("/actuator") || uri.startsWith("/swagger-ui") || uri.startsWith("/v3/api-docs");
    }
}
