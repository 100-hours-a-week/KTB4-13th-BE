package com.book.core.point.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.domain.PointBalance;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RecoverPointBalanceUseCaseTest {
    private final PointBalanceRepositoryPort repository = mock(PointBalanceRepositoryPort.class);

    @Test
    void 포인트_잔액이_없으면_생성한다() {
        when(repository.existsByUserId(42L)).thenReturn(false);

        final boolean recovered = new RecoverPointBalanceUseCase(repository).execute(42L);

        final ArgumentCaptor<PointBalance> captor = ArgumentCaptor.forClass(PointBalance.class);
        verify(repository).save(captor.capture());
        assertThat(recovered).isTrue();
        assertThat(captor.getValue().userId()).isEqualTo(42L);
    }

    @Test
    void 포인트_잔액이_이미_있으면_생성하지_않는다() {
        when(repository.existsByUserId(42L)).thenReturn(true);

        final boolean recovered = new RecoverPointBalanceUseCase(repository).execute(42L);

        assertThat(recovered).isFalse();
        verify(repository, never()).save(any(PointBalance.class));
    }
}
