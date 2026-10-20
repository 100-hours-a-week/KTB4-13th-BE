package com.book.core.coupon.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.book.core.coupon.application.port.CouponRepositoryPort;
import com.book.core.coupon.application.port.OwnedCouponRepositoryPort;
import com.book.core.coupon.domain.Coupon;
import com.book.core.coupon.domain.OwnedCoupon;
import com.book.core.coupon.domain.enums.CouponType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataAccessResourceFailureException;

class IssueCouponLoggingTest {
    private final CouponRepositoryPort coupons = mock(CouponRepositoryPort.class);
    private final OwnedCouponRepositoryPort ownedCoupons = mock(OwnedCouponRepositoryPort.class);
    private final List<Logger> loggers = List.of((Logger) LoggerFactory.getLogger(IssueCouponUseCase.class));
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
        MDC.put("traceId", "coupon-test-trace");
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
    void 발급은_시작과_처리_종료를_식별자와_함께_기록한다() {
        final var coupon =
            new Coupon(10L, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, BigDecimal.ZERO, LocalDateTime.now().plusDays(1));
        when(coupons.findIssuableById(eq(10L), any(LocalDateTime.class))).thenReturn(Optional.of(coupon));
        when(ownedCoupons.findByUserIdAndCouponId(42L, 10L)).thenReturn(Optional.empty());

        new IssueCouponUseCase(coupons, ownedCoupons).execute(42L, 10L);

        assertThat(appender.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.DEBUG, Level.INFO);
        assertThat(appender.list.subList(0, 2))
            .allSatisfy((final var event) -> assertThat(event.getFormattedMessage()).contains("쿠폰 발급", "couponId=10"));
        assertThat(appender.list).allSatisfy((final var event) -> assertThat(event.getFormattedMessage())
            .contains("traceId=coupon-test-trace").doesNotContain("userId=", "discount=", "minOrderAmount="));
    }

    @Test
    void 저장이_실패하면_처리_종료_INFO를_남기지_않는다() {
        final var coupon =
            new Coupon(10L, "쿠폰", CouponType.FIXED_AMOUNT, BigDecimal.TEN, 2L, BigDecimal.ZERO, LocalDateTime.now().plusDays(1));
        final var failure = new DataAccessResourceFailureException("database failure");
        when(coupons.findIssuableById(eq(10L), any(LocalDateTime.class))).thenReturn(Optional.of(coupon));
        when(ownedCoupons.save(any(OwnedCoupon.class))).thenThrow(failure);

        assertThatThrownBy(() -> new IssueCouponUseCase(coupons, ownedCoupons).execute(42L, 10L)).isSameAs(failure);

        assertThat(appender.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.DEBUG);
        assertThat(appender.list)
            .allSatisfy((final var event) -> assertThat(event.getFormattedMessage()).contains("처리를 시작합니다.").doesNotContain("처리를 마쳤습니다."));
    }
}
