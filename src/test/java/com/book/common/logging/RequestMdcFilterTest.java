package com.book.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.filter.RequestMdcFilter;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestMdcFilterTest {
    private final RequestMdcFilter filter = new RequestMdcFilter();

    @AfterEach
    void clearMdc() {
        MDC.remove(RequestMdcFilter.REQUEST_ID_MDC_KEY);
        MDC.remove(RequestMdcFilter.REQUEST_METHOD_MDC_KEY);
        MDC.remove(RequestMdcFilter.REQUEST_URI_MDC_KEY);
        MDC.remove(RequestMdcFilter.CLIENT_IP_MDC_KEY);
        MDC.remove("existingContext");
    }

    @Test
    void 요청_ID가_없으면_UUID를_생성해_MDC와_응답_header에_설정한다() throws ServletException, IOException {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/books");
        request.setRemoteAddr("192.0.2.10");
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (final var servletRequest, final var servletResponse) -> {
            assertThat(MDC.get(RequestMdcFilter.REQUEST_ID_MDC_KEY)).isNotBlank();
            assertThat(MDC.get(RequestMdcFilter.REQUEST_METHOD_MDC_KEY)).isEqualTo("GET");
            assertThat(MDC.get(RequestMdcFilter.REQUEST_URI_MDC_KEY)).isEqualTo("/api/v1/books");
            assertThat(MDC.get(RequestMdcFilter.CLIENT_IP_MDC_KEY)).isEqualTo("192.0.2.10");
        });

        final String requestId = response.getHeader(RequestMdcFilter.REQUEST_ID_HEADER);
        assertThat(requestId).matches("[0-9a-f]{32}");
        assertMdcIsCleared();
    }

    @Test
    void 요청_header의_ID를_MDC와_응답_header에_그대로_사용한다() throws ServletException, IOException {
        final String requestId = "request-abc-123";
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/books");
        request.addHeader(RequestMdcFilter.REQUEST_ID_HEADER, requestId);
        final MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (final var servletRequest,
            final var servletResponse) -> assertThat(MDC.get(RequestMdcFilter.REQUEST_ID_MDC_KEY)).isEqualTo(requestId));

        assertThat(response.getHeader(RequestMdcFilter.REQUEST_ID_HEADER)).isEqualTo(requestId);
        assertMdcIsCleared();
    }

    @Test
    void 요청_처리가_예외로_끝나도_필터가_설정한_MDC만_정리한다() {
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/books");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put("existingContext", "preserve");

        assertThatThrownBy(() -> filter.doFilter(request, response, (final var servletRequest, final var servletResponse) -> {
            assertThat(MDC.get(RequestMdcFilter.REQUEST_ID_MDC_KEY)).isNotBlank();
            throw new ServletException("request failed");
        })).isInstanceOf(ServletException.class).hasMessage("request failed");

        assertMdcIsCleared();
        assertThat(MDC.get("existingContext")).isEqualTo("preserve");
    }

    private void assertMdcIsCleared() {
        assertThat(MDC.get(RequestMdcFilter.REQUEST_ID_MDC_KEY)).isNull();
        assertThat(MDC.get(RequestMdcFilter.REQUEST_METHOD_MDC_KEY)).isNull();
        assertThat(MDC.get(RequestMdcFilter.REQUEST_URI_MDC_KEY)).isNull();
        assertThat(MDC.get(RequestMdcFilter.CLIENT_IP_MDC_KEY)).isNull();
    }
}
