package com.book.core.point.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.book.common.exception.CoreException;
import com.book.core.point.application.command.EarnPointCommand;
import com.book.core.point.application.command.GetPointCommand;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
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
    private final List<Logger> loggers =
        List.of((Logger) LoggerFactory.getLogger(GetPointUseCase.class), (Logger) LoggerFactory.getLogger(EarnPointUseCase.class));
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

    @Test
    void 변경은_시작과_처리_종료를_기록하고_금액을_노출하지_않는다() {
        when(balances.findByUserId(42L)).thenReturn(Optional.of(new PointBalance(42L)));

        executeChange(new BigDecimal("123.45"));

        assertThat(appender.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.DEBUG, Level.INFO);
        assertThat(appender.list.getFirst().getFormattedMessage()).contains("처리를 시작합니다.");
        assertThat(appender.list.getLast().getFormattedMessage()).contains("처리를 마쳤습니다.");
        assertThat(appender.list).allSatisfy((final var event) -> assertThat(event.getFormattedMessage())
            .contains("type=REVIEW", "referenceId=10", "traceId=point-test-trace")
            .doesNotContain("123.45", "amount=", "balanceAfter=", "userId="));
    }

    @Test
    void 금액이_0이면_생략_로그_하나만_남기고_저장소를_호출하지_않는다() {
        executeChange(new BigDecimal("0.00"));

        verifyNoInteractions(balances, histories);
        assertThat(appender.list).singleElement().satisfies((final var event) -> {
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(event.getFormattedMessage()).contains("생략합니다.", "traceId=point-test-trace").doesNotContain("처리를 시작합니다.");
        });
    }

    @Test
    void 하위_계층은_업무_오류를_다시_기록하지_않는다() {
        when(balances.findByUserId(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> executeChange(BigDecimal.TEN)).isInstanceOf(CoreException.class);

        assertThat(appender.list).singleElement().satisfies((final var event) -> {
            assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
            assertThat(event.getThrowableProxy()).isNull();
            assertThat(event.getFormattedMessage()).doesNotContain("errorCode=", "완료");
        });
    }

    @Test
    void INFO_수준에서는_변경_처리_종료만_기록한다() {
        for (final Logger logger : loggers) {
            logger.setLevel(Level.INFO);
        }
        when(balances.findByUserId(42L)).thenReturn(Optional.of(new PointBalance(42L)));
        when(histories.findByUserId(42L)).thenReturn(List.of());

        new GetPointUseCase(balances, histories).execute(new GetPointCommand(42L));
        executeChange(BigDecimal.TEN);
        executeChange(BigDecimal.ZERO);

        assertThat(appender.list).singleElement().satisfies((final var event) -> {
            assertThat(event.getLevel()).isEqualTo(Level.INFO);
            assertThat(event.getFormattedMessage()).contains("포인트 적립 처리를 마쳤습니다.");
        });
    }

    private void executeChange(final BigDecimal amount) {
        new EarnPointUseCase(balances, histories).execute(new EarnPointCommand(42L, PointType.REVIEW, 10L, amount));
    }
}
