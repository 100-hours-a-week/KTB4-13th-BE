package com.book.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = LoggingProbeConfiguration.class)
@ActiveProfiles("dev")
class LogbackStructuredLoggingTest {
    @Test
    @SuppressWarnings("unchecked")
    void 배포_프로필은_JSON_콘솔에_requestId와_요청_필드를_직렬화한다() throws Exception {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        final Logger bookLogger = context.getLogger("com.book");
        final Logger springWebLogger = context.getLogger("org.springframework.web");
        final Logger springSecurityLogger = context.getLogger("org.springframework.security");
        final Appender<ILoggingEvent> jsonAppender = rootLogger.getAppender("CONSOLE_JSON");

        assertThat(jsonAppender).isInstanceOf(OutputStreamAppender.class);
        assertThat(rootLogger.getAppender("CONSOLE")).isNull();
        assertThat(bookLogger.getLevel().levelStr).isEqualTo("DEBUG");
        assertThat(springWebLogger.getLevel().levelStr).isEqualTo("DEBUG");
        assertThat(springSecurityLogger.getLevel().levelStr).isEqualTo("DEBUG");
        final Encoder<ILoggingEvent> encoder = ((OutputStreamAppender<ILoggingEvent>) jsonAppender).getEncoder();
        assertThat(encoder.getClass().getName()).isEqualTo("org.springframework.boot.logging.logback.StructuredLogEncoder");

        final Logger logger = context.getLogger("com.book.common.logging.StructuredLogProbe");
        final Level previousLevel = logger.getLevel();
        final boolean previousAdditive = logger.isAdditive();
        final Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        final ListAppender<ILoggingEvent> eventAppender = new ListAppender<>();
        eventAppender.setContext(context);
        eventAppender.start();
        logger.setLevel(Level.INFO);
        logger.setAdditive(false);
        logger.addAppender(eventAppender);
        MDC.put("requestId", "request-json-test");
        MDC.put("requestMethod", "POST");
        MDC.put("requestUri", "/api/v1/coupons/7/use");
        MDC.put("responseStatus", "204");
        MDC.put("durationMs", "12");

        try {
            logger.atInfo().addKeyValue("ownedCouponId", 7L).log("[OWNED_COUPON] 쿠폰 사용 처리를 마쳤습니다. ownedCouponId={}", 7L);

            final String json = new String(encoder.encode(eventAppender.list.getFirst()), StandardCharsets.UTF_8);
            final var jsonLog = new ObjectMapper().readTree(json);
            assertThat(jsonLog.path("requestId").asText()).isEqualTo("request-json-test");
            assertThat(jsonLog.path("requestMethod").asText()).isEqualTo("POST");
            assertThat(jsonLog.path("requestUri").asText()).isEqualTo("/api/v1/coupons/7/use");
            assertThat(jsonLog.path("responseStatus").asText()).isEqualTo("204");
            assertThat(jsonLog.path("durationMs").asText()).isEqualTo("12");
            assertThat(jsonLog.path("ownedCouponId").asText()).isEqualTo("7");
            assertThat(json).doesNotContain("clientIp", "traceId");
        } finally {
            logger.detachAppender(eventAppender);
            logger.setLevel(previousLevel);
            logger.setAdditive(previousAdditive);
            eventAppender.stop();
            MDC.clear();
            if (previousMdc != null) {
                MDC.setContextMap(previousMdc);
            }
        }
    }
}
