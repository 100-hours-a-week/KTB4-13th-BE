package com.book.core.review.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.book.common.exception.CoreException;
import com.book.common.paging.OffsetLimit;
import com.book.common.paging.Page;
import com.book.core.review.domain.ReviewTarget;
import com.book.core.review.domain.ReviewContent;
import com.book.core.review.application.result.ReviewItemResult;
import com.book.core.review.application.port.ReviewRepositoryPort;
import com.book.core.review.domain.Review;
import com.book.core.review.domain.ReviewKey;
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
    private final List<Logger> loggers = List.of(logger(GetReviewsUseCase.class), logger(AddReviewUseCase.class),
        logger(UpdateReviewUseCase.class), logger(DeleteReviewUseCase.class));
    private List<Level> previousLevels;
    private List<Boolean> previousAdditives;
    private Map<String, String> previousMdc;

    @BeforeEach
    void captureLogs() {
        when(repository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
    void 조회_파라미터와_결과_요약을_INFO로_기록한다() {
        final var paging = new OffsetLimit(3, 2);
        when(repository.findActiveReviews(new ReviewTarget(ReviewTargetType.PRODUCT, 10L), paging))
            .thenReturn(new Page<>(List.of(new ReviewItemResult(801L, new ReviewTarget(ReviewTargetType.PRODUCT, 10L),
                new BigDecimal("4.5"), "private-review-content", true)), true));

        new GetReviewsUseCase(repository).execute(new ReviewTarget(ReviewTargetType.PRODUCT, 10L), paging);

        assertThat(appender.list).extracting(ILoggingEvent::getLevel).containsExactly(Level.INFO);
        assertThat(appender.list.getFirst().getFormattedMessage())
            .contains("targetType=PRODUCT", "targetId=10", "offset=3", "limit=2", "count=1", "hasNext=true", "traceId=review-test-trace")
            .doesNotContain("private-review-content", "rate=", "userId=");
    }

    @Test
    void 작성_본문_끝에서_INFO를_기록하고_내용과_평점을_제외한다() {
        new AddReviewUseCase(repository).execute(new ReviewTarget(ReviewTargetType.PRODUCT, 10L), new ReviewKey(42L, "ORDER_ITEM_701"),
            new ReviewContent(new BigDecimal("4.5"), "private-review-content"), false);

        assertWriteLogs("targetId=10");
    }

    @Test
    void 수정_본문_끝에서_INFO를_기록한다() {
        when(repository.findActiveById(801L)).thenReturn(Optional.of(review()));

        new UpdateReviewUseCase(repository).execute(42L, 801L, new ReviewContent(new BigDecimal("4.5"), "private-review-content"), false);

        assertWriteLogs("reviewId=801");
    }

    @Test
    void 삭제_본문_끝에서_INFO를_기록한다() {
        when(repository.findActiveById(801L)).thenReturn(Optional.of(review()));

        new DeleteReviewUseCase(repository).execute(42L, 801L);

        assertWriteLogs("reviewId=801");
    }

    @Test
    void 업무_오류는_하위_계층에서_다시_기록하지_않고_종료_INFO도_남기지_않는다() {
        when(repository.findActiveById(801L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new DeleteReviewUseCase(repository).execute(42L, 801L)).isInstanceOf(CoreException.class);

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
        return Review.create(new ReviewKey(42L, "ORDER_ITEM_" + 701L), new ReviewTarget(ReviewTargetType.PRODUCT, 10L),
            new ReviewContent(new BigDecimal("4.5"), "private-review-content"), false);
    }

    private static Logger logger(final Class<?> type) {
        return (Logger) LoggerFactory.getLogger(type);
    }
}
