package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.book.common.filter.RequestMdcFilter;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.domain.OwnedCoupon;
import com.book.core.coupon.domain.enums.OwnedCouponStatus;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessResourceFailureException;

class CouponUseCaseLoggingTest {
    private final OwnedCouponRepositoryPort ownedCoupons = mock(OwnedCouponRepositoryPort.class);
    private final List<Logger> loggers = List.of((Logger) LoggerFactory.getLogger(UseOwnedCouponUseCase.class),
        (Logger) LoggerFactory.getLogger(RevertOwnedCouponUseCase.class));
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
        MDC.put(RequestMdcFilter.REQUEST_ID_MDC_KEY, "coupon-test-request");
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
    void 사용_복원은_시작과_처리_종료를_식별자와_함께_기록한다() {
        when(ownedCoupons.findById(1L)).thenReturn(Optional.of(new OwnedCoupon(42L, 10L, OwnedCouponStatus.ISSUED, 2L, 0L)));

        new UseOwnedCouponUseCase(ownedCoupons).execute(1L);
        new RevertOwnedCouponUseCase(ownedCoupons).execute(1L);

        assertThat(appender.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.DEBUG, Level.INFO, Level.DEBUG, Level.INFO);
        assertThat(appender.list.subList(0, 2))
            .allSatisfy((final var event) -> assertThat(event.getFormattedMessage()).contains("쿠폰 사용", "ownedCouponId=1"));
        assertThat(appender.list.subList(2, 4))
            .allSatisfy((final var event) -> assertThat(event.getFormattedMessage()).contains("쿠폰 복원", "ownedCouponId=1"));
        assertThat(appender.list).allSatisfy((final var event) -> assertThat(event.getFormattedMessage())
            .contains("requestId=coupon-test-request").doesNotContain("traceId=", "userId=", "discount=", "minOrderAmount="));
    }

    @Test
    void 조회가_실패하면_처리_종료_INFO를_남기지_않는다() {
        final var failure = new DataAccessResourceFailureException("database failure");
        when(ownedCoupons.findById(1L)).thenThrow(failure);

        assertThatThrownBy(() -> new UseOwnedCouponUseCase(ownedCoupons).execute(1L)).isSameAs(failure);
        assertThatThrownBy(() -> new RevertOwnedCouponUseCase(ownedCoupons).execute(1L)).isSameAs(failure);

        assertThat(appender.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.DEBUG, Level.DEBUG);
        assertThat(appender.list)
            .allSatisfy((final var event) -> assertThat(event.getFormattedMessage()).contains("처리를 시작합니다.").doesNotContain("처리를 마쳤습니다."));
    }
}
