package com.book.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.book.common.filter.RequestMdcFilter;
import java.sql.SQLException;
import java.util.Map;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.client.RestClientException;

class GlobalExceptionHandlerLoggingTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private Level previousLevel;
    private boolean previousAdditive;
    private Map<String, String> previousMdc;

    @BeforeEach
    void captureLogs() {
        previousLevel = logger.getLevel();
        previousAdditive = logger.isAdditive();
        previousMdc = MDC.getCopyOfContextMap();
        logger.setLevel(Level.TRACE);
        logger.setAdditive(false);
        appender.start();
        logger.addAppender(appender);
        MDC.put(RequestMdcFilter.REQUEST_ID_MDC_KEY, "test-request-id");
    }

    @AfterEach
    void restoreLogging() {
        logger.detachAppender(appender);
        appender.stop();
        logger.setLevel(previousLevel);
        logger.setAdditive(previousAdditive);
        MDC.clear();
        if (previousMdc != null) {
            MDC.setContextMap(previousMdc);
        }
    }

    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void 업무_오류는_정의된_레벨로_한_번_기록하고_기존_응답을_유지한다(final ErrorCode errorCode) {
        final var response = handler.handleCoreException(new CoreException(errorCode, "private-data"));

        assertThat(appender.list).hasSize(1);
        final var event = appender.list.getFirst();
        assertThat(event.getLevel().levelStr).isEqualTo(errorCode.logLevel().name());
        assertThat(event.getFormattedMessage()).contains("[GLOBAL_EXCEPTION]", "errorCode=" + errorCode.name(), "code=" + errorCode.code())
            .doesNotContain("private-data", "requestId=");
        assertThat(event.getMDCPropertyMap()).containsEntry("requestId", "test-request-id");
        assertThat(event.getThrowableProxy()).isNull();
        assertThat(response.getStatusCode()).isEqualTo(errorCode.status());
        assertThat(response.getBody().data()).isEqualTo("private-data");
        assertThat(response.getBody().traceId()).isEqualTo("test-request-id");
    }

    @Test
    void 기술_오류는_예외와_원인을_한_번_기록하고_안전한_응답을_유지한다() {
        final var cause = new IllegalStateException("database unavailable");
        final var exception = new DataAccessResourceFailureException("connection failed", cause);

        final var response = handler.handleUnexpected(exception);

        assertThat(appender.list).hasSize(1);
        final var event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage()).contains("[GLOBAL_EXCEPTION]", "errorCode=DEFAULT_ERROR", "code=E500")
            .doesNotContain("requestId=");
        assertThat(event.getMDCPropertyMap()).containsEntry("requestId", "test-request-id");
        assertThat(event.getThrowableProxy().getClassName()).isEqualTo(exception.getClass().getName());
        assertThat(event.getThrowableProxy().getCause().getClassName()).isEqualTo(cause.getClass().getName());
        assertThat(response.getBody().message()).isEqualTo(ErrorCode.DEFAULT_ERROR.message());
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    void 입력_검증_오류는_요청_원문_없이_한_번_기록한다() {
        final var exception = new HttpMessageNotReadableException("private-request-body", new MockHttpInputMessage(new byte[0]));

        final var response = handler.handleInvalidRequest(exception);

        assertThat(appender.list).hasSize(1);
        final var event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(event.getFormattedMessage()).contains("[GLOBAL_EXCEPTION]", "errorCode=INVALID_REQUEST")
            .doesNotContain("private-request-body", "requestId=");
        assertThat(event.getMDCPropertyMap()).containsEntry("requestId", "test-request-id");
        assertThat(event.getThrowableProxy()).isNull();
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.INVALID_REQUEST.status());
    }

    @Test
    void 알_수_없는_제약_오류는_예외와_원인을_기록하고_안전한_응답을_유지한다() {
        final var cause = new IllegalStateException("database unavailable");
        final var exception = new DataIntegrityViolationException("connection failed", cause);

        final var response = handler.handleConstraintConflict(exception);

        assertThat(appender.list).hasSize(1);
        final var event = appender.list.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage()).contains("[GLOBAL_EXCEPTION]", "errorCode=DEFAULT_ERROR", "code=E500")
            .doesNotContain("requestId=");
        assertThat(event.getMDCPropertyMap()).containsEntry("requestId", "test-request-id");
        assertThat(event.getThrowableProxy().getClassName()).isEqualTo(exception.getClass().getName());
        assertThat(event.getThrowableProxy().getCause().getClassName()).isEqualTo(cause.getClass().getName());
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.DEFAULT_ERROR.status());
        assertThat(response.getBody().data()).isNull();
    }

    @Test
    void 외부_응답_예외는_응답_원문과_스택_트레이스를_로그에_남기지_않는다() {
        final var exception = new IllegalStateException("외부 요청 실패", new RestClientException("private-external-response-body"));

        handler.handleUnexpected(exception);

        assertThat(appender.list).singleElement().satisfies((final var event) -> {
            assertThat(event.getLevel()).isEqualTo(Level.ERROR);
            assertThat(event.getFormattedMessage())
                .contains("[GLOBAL_EXCEPTION]", "errorCode=DEFAULT_ERROR", "errorType=IllegalStateException")
                .doesNotContain("private-external-response-body", "requestId=");
            assertThat(event.getMDCPropertyMap()).containsEntry("requestId", "test-request-id");
            assertThat(event.getThrowableProxy()).isNull();
        });
    }

    @Test
    void 알려진_제약_경합은_충돌_로그를_한_번_기록하고_409_응답을_유지한다() {
        final var cause =
            new ConstraintViolationException("duplicate", new SQLException("duplicate", "23000", 1062), "uk_chat_request_key");
        final var exception = new DataIntegrityViolationException("database details", cause);

        final var response = handler.handleConstraintConflict(exception);

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getLevel()).isEqualTo(Level.INFO);
        assertThat(appender.list.getFirst().getFormattedMessage()).contains("[GLOBAL_EXCEPTION]", "errorCode=CONCURRENT_CHANGE")
            .doesNotContain("requestId=");
        assertThat(appender.list.getFirst().getMDCPropertyMap()).containsEntry("requestId", "test-request-id");
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.CONCURRENT_CHANGE.status());
        assertThat(response.getBody().code()).isEqualTo(ErrorCode.CONCURRENT_CHANGE.code());
    }

    @Test
    void 재시도_오류는_한_번_기록하고_Retry_After_헤더를_유지한다() {
        final var response = handler.handleRetryAfter(new RetryAfterException(ErrorCode.AI_RECOMMENDATION_RATE_LIMITED, 30));

        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.getFirst().getLevel()).isEqualTo(Level.INFO);
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("30");
    }
}
