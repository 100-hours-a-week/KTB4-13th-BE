package com.book.core.point.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.domain.PointBalance;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetPointBalanceUseCaseTest {
    @Test
    void 포인트_이력_없이_잔액을_조회하고_없는_회원은_거부한다() {
        final PointBalanceRepositoryPort repository = mock(PointBalanceRepositoryPort.class);
        final GetPointBalanceUseCase useCase = new GetPointBalanceUseCase(repository);
        when(repository.findByUserId(42L)).thenReturn(Optional.of(new PointBalance(1L, 42L, BigDecimal.TEN)));
        assertThat(useCase.execute(42L)).isEqualByComparingTo("10");
        when(repository.findByUserId(42L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> useCase.execute(42L)).isInstanceOf(CoreException.class);
    }
}
