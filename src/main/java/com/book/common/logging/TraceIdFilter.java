package com.book.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceIdFilter extends OncePerRequestFilter {
    public static final String TRACE_ID_MDC_KEY = "traceId";
    private static final String TRACE_ID_HEADER_KEY = "X-Trace-Id";
    private static final String REQUEST_METHOD_MDC_KEY = "requestMethod";
    private static final String REQUEST_URI_MDC_KEY = "requestUri";
    private static final String CLIENT_IP_MDC_KEY = "clientIp";
    private static final String HEALTH_CHECK_PATH = "/api/v1/support/health";
    private static final String SWAGGER_UI_PATH = "/swagger-ui/";
    private static final String OPENAPI_PATH = "/v3/api-docs";
    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
        throws ServletException, IOException {
        final String traceId = UUID.randomUUID().toString();
        final String requestUri = request.getRequestURI();
        MDC.put(TRACE_ID_MDC_KEY, traceId);
        MDC.put(REQUEST_METHOD_MDC_KEY, request.getMethod());
        MDC.put(REQUEST_URI_MDC_KEY, requestUri);
        // Forwarded headers remain untrusted unless the servlet container is configured with a trusted
        // proxy.
        MDC.put(CLIENT_IP_MDC_KEY, request.getRemoteAddr());
        response.setHeader(TRACE_ID_HEADER_KEY, traceId);
        final long startTime = System.nanoTime();
        int responseStatus = response.getStatus();
        try {
            filterChain.doFilter(request, response);
            responseStatus = response.getStatus();
        } catch (IOException | ServletException | RuntimeException exception) {
            responseStatus = response.getStatus();
            if (responseStatus < HttpServletResponse.SC_BAD_REQUEST) {
                responseStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }
            throw exception;
        } finally {
            if (shouldLogRequest(requestUri)) {
                final long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                log.info("[API_REQUEST] traceId={} method={} uri={} status={} durationMs={}", traceId, request.getMethod(), requestUri,
                    responseStatus, durationMs);
            }
            MDC.remove(CLIENT_IP_MDC_KEY);
            MDC.remove(REQUEST_URI_MDC_KEY);
            MDC.remove(REQUEST_METHOD_MDC_KEY);
            MDC.remove(TRACE_ID_MDC_KEY);
        }
    }

    private boolean shouldLogRequest(final String requestUri) {
        return !requestUri.equals(HEALTH_CHECK_PATH) && !requestUri.equals("/swagger-ui.html") && !requestUri.startsWith(SWAGGER_UI_PATH)
            && !requestUri.equals(OPENAPI_PATH) && !requestUri.startsWith(OPENAPI_PATH + "/") && !requestUri.equals(OPENAPI_PATH + ".yaml");
    }
}
