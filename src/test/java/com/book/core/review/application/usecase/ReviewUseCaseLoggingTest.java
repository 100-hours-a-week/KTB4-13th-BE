package com.book.core.review.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.book.common.exception.CoreException;
import com.book.common.paging.OffsetLimit;
import com.book.common.paging.Page;
import com.book.core.order.application.usecase.GetOrderItemUseCase;
import com.book.core.order.domain.Order;
import com.book.core.order.domain.OrderItem;
import com.book.core.order.domain.OrderItemStatus;
import com.book.core.review.application.command.CreateReviewCommand;
import com.book.core.review.application.command.DeleteReviewCommand;
import com.book.core.review.application.command.GetReviewsCommand;
import com.book.core.review.application.command.UpdateReviewCommand;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.enums.ReviewTargetType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class ReviewUseCaseLoggingTest {
    private final ReviewRepositoryPort repository = mock(ReviewRepositoryPort.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final List<Logger> loggers = List.of(logger(GetReviewsUseCase.class), logger(CreateReviewUseCase.class),
        logger(UpdateReviewUseCase.class), logger(DeleteReviewUseCase.class));
    private List<Level> previousLevels;
    private List<Boolean> previousAdditives;
    private Map<String, String> previousMdc;

    @BeforeEach
    void captureLogs() {
        previousLevels = loggers.stream().map(Logger::getLevel).toList();
        previousAdditives = loggers.stream().map(Logger::isAdditive).toList();
        previousMdc = MDC.getCopyOfContextMap();
        appender.start();
        for (final var logger : loggers) {
            logger.setLevel(Level.DEBUG);
            logger.setAdditive(false);
            logger.addAppender(appender);
        }
        MDC.put("traceId", "review-test-trace");
    }

    @AfterEach
    void restoreLogging() {
        for (int index = 0; index < loggers.size(); index++) {
            final var logger = loggers.get(index);
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
    void 조회는_DEBUG만_기록한다() {
        final var paging = new OffsetLimit(3, 2);
        when(repository.findActiveReviews(ReviewTargetType.PRODUCT, 10L, paging)).thenReturn(new Page<>(List.of(), false));

        new GetReviewsUseCase(repository).execute(new GetReviewsCommand(ReviewTargetType.PRODUCT, 10L, paging));

        assertThat(appender.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.DEBUG);
        assertThat(appender.list.getFirst().getFormattedMessage()).contains("targetType=PRODUCT", "targetId=10", "offset=3", "limit=2",
            "traceId=review-test-trace");
    }

    @Test
    void 작성_본문_끝에서_INFO를_기록하고_내용과_평점을_제외한다() {
        final var orders = mock(GetOrderItemUseCase.class);
        final var item = mock(OrderItem.class);
        final var order = mock(Order.class);
        when(item.order()).thenReturn(order);
        when(order.userId()).thenReturn(42L);
        when(item.status()).thenReturn(OrderItemStatus.PAID);
        when(item.productId()).thenReturn(10L);
        when(orders.execute(701L)).thenReturn(item);

        new CreateReviewUseCase(repository, orders)
            .execute(new CreateReviewCommand(42L, 701L, new BigDecimal("4.5"), "private-review-content", false));

        assertWriteLogs("orderItemId=701");
    }

    @Test
    void 수정_본문_끝에서_INFO를_기록한다() {
        when(repository.findActiveById(801L)).thenReturn(Optional.of(review()));

        new UpdateReviewUseCase(repository)
            .execute(new UpdateReviewCommand(42L, 801L, new BigDecimal("4.5"), "private-review-content", false));

        assertWriteLogs("reviewId=801");
    }

    @Test
    void 삭제_본문_끝에서_INFO를_기록한다() {
        when(repository.findActiveById(801L)).thenReturn(Optional.of(review()));

        new DeleteReviewUseCase(repository).execute(new DeleteReviewCommand(42L, 801L));

        assertWriteLogs("reviewId=801");
    }

    @Test
    void 업무_오류는_하위_계층에서_다시_기록하지_않고_종료_INFO도_남기지_않는다() {
        when(repository.findActiveById(801L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new DeleteReviewUseCase(repository).execute(new DeleteReviewCommand(42L, 801L)))
            .isInstanceOf(CoreException.class);

        assertThat(appender.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.DEBUG);
        assertThat(appender.list.getFirst().getThrowableProxy()).isNull();
    }

    private void assertWriteLogs(final String identifier) {
        assertThat(appender.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.DEBUG, Level.INFO);
        assertThat(appender.list).allSatisfy(event -> {
            assertThat(event.getFormattedMessage()).contains(identifier, "traceId=review-test-trace")
                .doesNotContain("private-review-content", "rate=", "rating=", "userId=", "reviewKey=");
            assertThat(event.getThrowableProxy()).isNull();
        });
    }

    private Review review() {
        return Review.create(42L, 701L, 10L, new BigDecimal("4.5"), "private-review-content", false);
    }

    private static Logger logger(final Class<?> type) {
        return (Logger) LoggerFactory.getLogger(type);
    }
}
