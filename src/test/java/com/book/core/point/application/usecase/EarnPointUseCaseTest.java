package com.book.core.point.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.application.port.PointHistoryRepositoryPort;
import com.book.core.point.domain.PointBalance;
import com.book.core.point.domain.PointHistory;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class EarnPointUseCaseTest {
    private final PointBalanceRepositoryPort balances = mock(PointBalanceRepositoryPort.class);
    private final PointHistoryRepositoryPort histories = mock(PointHistoryRepositoryPort.class);
    private final EarnPointUseCase useCase = new EarnPointUseCase(balances, histories);

    @ParameterizedTest
    @CsvSource({"250, 1250, 250", "-250, 750, -250"})
    void 잔액과_이력에_기존_금액_부호를_반영한다(final BigDecimal amount, final BigDecimal expectedBalance, final BigDecimal expectedHistoryAmount) {
        final PointBalance balance = new PointBalance(1L, 42L, new BigDecimal("1000"));
        when(balances.findByUserIdForUpdate(42L)).thenReturn(Optional.of(balance));
        when(balances.availableBalance(balance)).thenReturn(balance.balance());

        useCase.execute(42L, PointType.REVIEW, 10L, amount);

        final ArgumentCaptor<PointHistory> captured = ArgumentCaptor.forClass(PointHistory.class);
        verify(histories).save(captured.capture());
        final PointHistory history = captured.getValue();
        assertThat(balance.balance()).isEqualByComparingTo(expectedBalance);
        assertThat(history.id()).isNull();
        assertThat(history.userId()).isEqualTo(42L);
        assertThat(history.type()).isEqualTo(PointType.REVIEW);
        assertThat(history.referenceId()).isEqualTo(10L);
        assertThat(history.amount()).isEqualByComparingTo(expectedHistoryAmount);
        assertThat(history.balanceAfter()).isEqualByComparingTo(expectedBalance);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.0", "0.00"})
    void 자릿수가_달라도_0이면_조회하거나_이력을_저장하지_않는다(final String amount) {
        useCase.execute(42L, PointType.REVIEW, 10L, new BigDecimal(amount));

        verifyNoInteractions(balances, histories);
    }

    @Test
    void 잔액이_없으면_오류를_반환하고_이력을_저장하지_않는다() {
        when(balances.findByUserIdForUpdate(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(42L, PointType.REVIEW, 10L, BigDecimal.TEN)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.NOT_FOUND_DATA));
        verifyNoInteractions(histories);
    }

    @Test
    void 음수_조정은_예약된_잔액을_침범할_수_없다() {
        final PointBalance balance = new PointBalance(1L, 42L, new BigDecimal("1000"));
        balance.reserve(new BigDecimal("700"));
        when(balances.findByUserIdForUpdate(42L)).thenReturn(Optional.of(balance));
        when(balances.availableBalance(balance)).thenReturn(balance.available());

        assertThatThrownBy(() -> useCase.execute(42L, PointType.REVIEW, 10L, new BigDecimal("-301"))).isInstanceOfSatisfying(
            CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.POINT_EXCEEDS_BALANCE));
        assertThat(balance.balance()).isEqualByComparingTo("1000");
        assertThat(balance.reserved()).isEqualByComparingTo("700");
        verifyNoInteractions(histories);
    }

}
