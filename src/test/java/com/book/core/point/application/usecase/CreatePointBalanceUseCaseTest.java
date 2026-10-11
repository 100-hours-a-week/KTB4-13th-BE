package com.book.core.point.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.domain.PointBalance;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreatePointBalanceUseCaseTest {
    @Test
    void 신규_회원의_포인트_잔액을_0으로_생성한다() {
        final PointBalanceRepositoryPort repository = mock(PointBalanceRepositoryPort.class);

        new CreatePointBalanceUseCase(repository).execute(42L);

        final ArgumentCaptor<PointBalance> captor = ArgumentCaptor.forClass(PointBalance.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().id()).isNull();
        assertThat(captor.getValue().userId()).isEqualTo(42L);
        assertThat(captor.getValue().balance()).isEqualByComparingTo("0");
    }
}
