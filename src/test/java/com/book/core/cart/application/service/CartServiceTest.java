package com.book.core.cart.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.book.core.cart.application.usecase.AddCartItemUseCase;
import com.book.core.cart.application.usecase.DeleteCartItemUseCase;
import com.book.core.cart.application.usecase.DeleteCartItemsUseCase;
import com.book.core.cart.application.usecase.FindUserIdsWithoutCartUseCase;
import com.book.core.cart.application.usecase.GetCartUseCase;
import com.book.core.cart.application.usecase.ModifyCartItemUseCase;
import com.book.core.cart.application.usecase.RecoverCartUseCase;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;

class CartServiceTest {
    private static final LocalDateTime REGISTERED_BEFORE = LocalDateTime.of(2030, 1, 1, 0, 0);

    private final FindUserIdsWithoutCartUseCase findUseCase = mock(FindUserIdsWithoutCartUseCase.class);
    private final RecoverCartUseCase recoverUseCase = mock(RecoverCartUseCase.class);
    private final CartService service = new CartService(mock(AddCartItemUseCase.class), mock(DeleteCartItemUseCase.class),
        mock(DeleteCartItemsUseCase.class), mock(GetCartUseCase.class), mock(ModifyCartItemUseCase.class), findUseCase, recoverUseCase);
    private final Logger logger = (Logger) LoggerFactory.getLogger(CartService.class);
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void restoreLogging() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void 장바구니_복구가_실패한_회원을_건너뛰고_다음_페이지까지_복구한_뒤_결과를_기록한다() {
        when(findUseCase.execute(REGISTERED_BEFORE, 0L, 2)).thenReturn(List.of(1L, 2L));
        when(findUseCase.execute(REGISTERED_BEFORE, 2L, 2)).thenReturn(List.of(3L));
        when(recoverUseCase.execute(1L)).thenThrow(new DataAccessResourceFailureException("cart"));
        when(recoverUseCase.execute(2L)).thenReturn(true);
        when(recoverUseCase.execute(3L)).thenReturn(false);

        service.recoverMissingCarts(REGISTERED_BEFORE, 2);

        assertThat(appender.list).filteredOn((final var event) -> event.getLevel() == Level.WARN).singleElement()
            .satisfies((final var event) -> assertThat(event.getFormattedMessage())
                .isEqualTo("장바구니 누락 복구에 실패했습니다. userId=1, cause=DataAccessResourceFailureException"));
        assertThat(appender.list).filteredOn((final var event) -> event.getLevel() == Level.INFO).singleElement()
            .satisfies((final var event) -> assertThat(event.getFormattedMessage()).isEqualTo(
                "장바구니 누락 복구를 마쳤습니다. target=3, recovered=1, skipped=1, failed=1, failureCauses={DataAccessResourceFailureException=1}"));
    }
}
