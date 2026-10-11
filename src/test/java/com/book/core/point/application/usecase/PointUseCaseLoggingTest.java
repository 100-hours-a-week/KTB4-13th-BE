package com.book.core.point.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.book.core.point.application.command.GetPointCommand;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.domain.PointBalance;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class PointUseCaseLoggingTest {
    private final PointBalanceRepositoryPort balances = mock(PointBalanceRepositoryPort.class);
    private final PointHistoryRepositoryPort histories = mock(PointHistoryRepositoryPort.class);
    private final List<Logger> loggers = List.of((Logger) LoggerFactory.getLogger(GetPointUseCase.class));
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private List<Level> previousLevels;
    private List<Boolean> previousAdditives;
    private Map<String, String> previousMdc;

    @BeforeEach
    void captureLogs() {
        previousLevels = loggers.stream().map(Logger::getLevel).toList();
        previousAdditives = loggers.stream().map(Logger::isAdditive).toList();
        previousMdc = MDC.getCopyOfContextMap();
        appender.start();
        for (final Logger logger : loggers) {
            logger.setLevel(Level.DEBUG);
            logger.setAdditive(false);
            logger.addAppender(appender);
        }
        MDC.put("traceId", "point-test-trace");
    }

    @AfterEach
    void restoreLogging() {
        for (int index = 0; index < loggers.size(); index++) {
            final Logger logger = loggers.get(index);
            logger.detachAppender(appender);
            logger.setLevel(previousLevels.get(index));
            logger.setAdditive(previousAdditives.get(index));
        }
        appender.stop();
        MDC.clear();
        if (previousMdc != null) {
            MDC.setContextMap(previousMdc);
        }
    }

    @Test
    void 조회는_추적_값만_기록하고_잔액과_이력을_노출하지_않는다() {
        when(balances.findByUserId(42L)).thenReturn(Optional.of(new PointBalance(42L)));
        when(histories.findByUserId(42L)).thenReturn(List.of());

        new GetPointUseCase(balances, histories).execute(new GetPointCommand(42L));

        assertThat(appender.list).singleElement().satisfies((final var event) -> {
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(event.getFormattedMessage()).isEqualTo("포인트 조회를 시작합니다. traceId=point-test-trace");
        });
    }

}
