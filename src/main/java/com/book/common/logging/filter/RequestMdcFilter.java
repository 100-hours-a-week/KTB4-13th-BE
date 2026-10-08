package com.book.common.logging.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestMdcFilter extends OncePerRequestFilter {
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID_MDC_KEY = "requestId";
    public static final String REQUEST_METHOD_MDC_KEY = "requestMethod";
    public static final String REQUEST_URI_MDC_KEY = "requestUri";
    public static final String CLIENT_IP_MDC_KEY = "clientIp";

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
        throws ServletException, IOException {
        final String requestId = Optional.ofNullable(request.getHeader(REQUEST_ID_HEADER)).filter(value -> !value.isBlank())
            .orElseGet(() -> UUID.randomUUID().toString());
        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        MDC.put(REQUEST_METHOD_MDC_KEY, request.getMethod());
        MDC.put(REQUEST_URI_MDC_KEY, request.getRequestURI());
        MDC.put(CLIENT_IP_MDC_KEY, request.getRemoteAddr());

        try {
            response.setHeader(REQUEST_ID_HEADER, requestId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CLIENT_IP_MDC_KEY);
            MDC.remove(REQUEST_URI_MDC_KEY);
            MDC.remove(REQUEST_METHOD_MDC_KEY);
            MDC.remove(REQUEST_ID_MDC_KEY);
        }
    }
}
