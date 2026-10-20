package com.book.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.book.common.filter.RequestLoggingFilter;
import com.book.common.filter.RequestMdcFilter;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLoggingFilterTest {
    private final Logger logger = (Logger) LoggerFactory.getLogger(RequestLoggingFilter.class);
    private final MdcCapturingListAppender appender = new MdcCapturingListAppender();
    private Level previousLevel;
    private boolean previousAdditive;
    private Map<String, String> previousMdc;

    @BeforeEach
    void captureLogs() {
        previousLevel = logger.getLevel();
        previousAdditive = logger.isAdditive();
        previousMdc = MDC.getCopyOfContextMap();
        appender.setContext(logger.getLoggerContext());
        appender.start();
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);
        logger.addAppender(appender);
        MDC.put(RequestMdcFilter.REQUEST_ID_MDC_KEY, "request-filter-test");
        MDC.put(RequestMdcFilter.REQUEST_METHOD_MDC_KEY, "POST");
        MDC.put(RequestMdcFilter.REQUEST_URI_MDC_KEY, "/api/v1/coupons/7/use");
    }

    @AfterEach
    void restoreLogging() {
        logger.detachAppender(appender);
        logger.setLevel(previousLevel);
        logger.setAdditive(previousAdditive);
        appender.stop();
        MDC.clear();
        if (previousMdc != null) {
            MDC.setContextMap(previousMdc);
        }
    }

    @Test
    void 요청_완료_로그는_요청_정보를_남기고_임시_MDC_필드를_정리한다() throws Exception {
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/coupons/7/use");
        request.setQueryString("token=private-token");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final RequestLoggingFilter filter = new RequestLoggingFilter();

        filter.doFilter(request, response, (final var servletRequest, final var servletResponse) -> response.setStatus(204));

        assertThat(appender.list).singleElement().satisfies((final ILoggingEvent event) -> {
            assertThat(event.getFormattedMessage()).startsWith("[HTTP_REQUEST] 요청 처리가 완료되었습니다.")
                .contains("method=POST", "uri=/api/v1/coupons/7/use", "status=204", "durationMs=")
                .doesNotContain("private-token", "request-filter-test", "traceId=");
            assertThat(event.getMDCPropertyMap()).containsEntry("requestId", "request-filter-test").containsEntry("requestMethod", "POST")
                .containsEntry("requestUri", "/api/v1/coupons/7/use").containsEntry("responseStatus", "204").containsKey("durationMs")
                .doesNotContainKey("clientIp").doesNotContainKey("traceId");
        });
        assertThat(MDC.get("durationMs")).isNull();
        assertThat(MDC.get("responseStatus")).isNull();
    }

    private static final class MdcCapturingListAppender extends ListAppender<ILoggingEvent> {
        @Override
        protected void append(final ILoggingEvent event) {
            event.prepareForDeferredProcessing();
            super.append(event);
        }
    }
}
