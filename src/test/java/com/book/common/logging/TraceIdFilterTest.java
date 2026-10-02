package com.book.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(OutputCaptureExtension.class)
class TraceIdFilterTest {
    private final TraceIdFilter filter = new TraceIdFilter();

    @AfterEach
    void clearMdc() {
        MDC.remove(TraceIdFilter.TRACE_ID_MDC_KEY);
        MDC.remove("requestMethod");
        MDC.remove("requestUri");
        MDC.remove("clientIp");
        MDC.remove("existingContext");
    }

    @Test
    void api_요청을_기록하고_응답_header_traceId와_MDC를_요청동안_유지한다(final CapturedOutput output) throws ServletException, IOException {
        final String token = "sensitive-token";
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/books");
        request.setRemoteAddr("192.0.2.10");
        request.setQueryString("token=" + token);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        request.addHeader("X-Forwarded-For", "198.51.100.99");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put("existingContext", "preserve");

        filter.doFilter(request, response, (final var servletRequest, final var servletResponse) -> {
            assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isEqualTo(response.getHeader("X-Trace-Id"));
            assertThat(MDC.get("requestMethod")).isEqualTo("GET");
            assertThat(MDC.get("requestUri")).isEqualTo("/api/v1/books");
            assertThat(MDC.get("clientIp")).isEqualTo("192.0.2.10");
            ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_CREATED);
        });

        final String traceId = response.getHeader("X-Trace-Id");
        assertThat(output.toString()).contains("[API_REQUEST] traceId=" + traceId + " method=GET uri=/api/v1/books status=201 durationMs=")
            .doesNotContain(token);
        assertThat(MDC.get("existingContext")).isEqualTo("preserve");
        assertFilterMdcIsCleared();
    }

    @Test
    void 오류_응답_상태를_API_요청_로그에_기록한다(final CapturedOutput output) throws ServletException, IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/books");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (final var servletRequest, final var servletResponse) -> ((HttpServletResponse) servletResponse)
            .setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE));

        assertThat(output.toString()).contains("[API_REQUEST]").contains("status=503");
        assertFilterMdcIsCleared();
    }

    @Test
    void health와_OpenAPI_요청은_traceId를_유지하면서_API_요청_로그에서_제외한다(final CapturedOutput output) throws ServletException, IOException {
        final List<String> excludedUris = List.of("/api/v1/support/health", "/swagger-ui/index.html", "/swagger-ui.html", "/v3/api-docs",
            "/v3/api-docs/swagger-config", "/v3/api-docs.yaml");

        for (final String uri : excludedUris) {
            final MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
            final MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response,
                (final var servletRequest, final var servletResponse) -> assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNotBlank());

            assertThat(response.getHeader("X-Trace-Id")).isNotBlank();
        }

        assertThat(output.toString()).doesNotContain("[API_REQUEST]");
        assertFilterMdcIsCleared();
    }

    @Test
    void 요청_처리가_예외로_끝나면_오류_상태를_기록하고_필터가_설정한_MDC만_정리한다(final CapturedOutput output) {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/books");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put("existingContext", "preserve");

        assertThatThrownBy(() -> filter.doFilter(request, response, (final var servletRequest, final var servletResponse) -> {
            assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNotBlank();
            throw new ServletException("request failed");
        })).isInstanceOf(ServletException.class).hasMessage("request failed");

        assertThat(output.toString()).contains("[API_REQUEST]").contains("status=500");
        assertFilterMdcIsCleared();
        assertThat(MDC.get("existingContext")).isEqualTo("preserve");
    }

    private void assertFilterMdcIsCleared() {
        assertThat(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)).isNull();
        assertThat(MDC.get("requestMethod")).isNull();
        assertThat(MDC.get("requestUri")).isNull();
        assertThat(MDC.get("clientIp")).isNull();
    }
}
