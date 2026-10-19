package com.book.core.cancel.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.cancel.application.port.CancelBalanceRepositoryPort;
import com.book.core.cancel.domain.CancelBalance;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetCancelBalanceUseCaseTest {
    private final CancelBalanceRepositoryPort cancelBalanceRepository = mock(CancelBalanceRepositoryPort.class);
    private final GetCancelBalanceUseCase useCase = new GetCancelBalanceUseCase(cancelBalanceRepository);

    @Test
    void 주문의_취소_잔액을_조회하고_없으면_데이터_없음으로_실패한다() {
        final CancelBalance balance =
            new CancelBalance(1L, BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        when(cancelBalanceRepository.findByOrderId(1L)).thenReturn(Optional.of(balance));
        when(cancelBalanceRepository.findByOrderId(2L)).thenReturn(Optional.empty());

        assertThat(useCase.execute(1L)).isSameAs(balance);
        assertThatThrownBy(() -> useCase.execute(2L)).isInstanceOf(CoreException.class)
            .extracting(exception -> ((CoreException) exception).errorCode()).isEqualTo(ErrorCode.NOT_FOUND_DATA);
    }
}
