package com.book.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = LoggingProbeConfiguration.class)
@ActiveProfiles("test")
class LogbackHumanReadableLoggingTest {
    @Test
    void 테스트_프로필은_사람이_읽기_쉬운_콘솔과_개발자용_레벨을_사용한다() {
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        final Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        final Logger bookLogger = context.getLogger("com.book");

        assertThat(rootLogger.getLevel().levelStr).isEqualTo("INFO");
        assertThat(rootLogger.getAppender("CONSOLE")).isNotNull();
        assertThat(rootLogger.getAppender("CONSOLE_JSON")).isNull();
        assertThat(bookLogger.getLevel().levelStr).isEqualTo("DEBUG");
    }
}
