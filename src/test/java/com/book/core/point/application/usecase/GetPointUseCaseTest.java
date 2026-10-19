package com.book.core.point.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.core.point.application.command.GetPointCommand;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.PointHistory;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetPointUseCaseTest {
    private final PointBalanceRepositoryPort balances = mock(PointBalanceRepositoryPort.class);
    private final PointHistoryRepositoryPort histories = mock(PointHistoryRepositoryPort.class);
    private final GetPointUseCase useCase = new GetPointUseCase(balances, histories);

    @Test
    void 회원의_잔액과_이력을_조회_결과로_반환한다() {
        when(balances.findByUserId(42L)).thenReturn(Optional.of(new PointBalance(1L, 42L, new BigDecimal("1250"))));
        when(histories.findByUserId(42L))
            .thenReturn(List.of(new PointHistory(2L, 42L, PointType.REVIEW, 10L, new BigDecimal("250"), new BigDecimal("1250"))));

        final var result = useCase.execute(new GetPointCommand(42L));

        assertThat(result.userId()).isEqualTo(42L);
        assertThat(result.balance()).isEqualByComparingTo("1250");
        assertThat(result.histories()).singleElement().satisfies((final var history) -> {
            assertThat(history.id()).isEqualTo(2L);
            assertThat(history.userId()).isEqualTo(42L);
            assertThat(history.type()).isEqualTo(PointType.REVIEW);
            assertThat(history.referenceId()).isEqualTo(10L);
            assertThat(history.amount()).isEqualByComparingTo("250");
            assertThat(history.createdAt()).isNull();
        });
    }

    @Test
    void 이력이_없으면_빈_목록을_반환한다() {
        when(balances.findByUserId(42L)).thenReturn(Optional.of(new PointBalance(42L)));
        when(histories.findByUserId(42L)).thenReturn(List.of());

        assertThat(useCase.execute(new GetPointCommand(42L)).histories()).isEmpty();
    }

    @Test
    void 잔액이_아직_없으면_0포인트로_조회한다() {
        when(balances.findByUserId(42L)).thenReturn(Optional.empty());
        when(histories.findByUserId(42L)).thenReturn(List.of());

        final var result = useCase.execute(new GetPointCommand(42L));

        assertThat(result.userId()).isEqualTo(42L);
        assertThat(result.balance()).isEqualByComparingTo("0");
        assertThat(result.histories()).isEmpty();
    }
}
