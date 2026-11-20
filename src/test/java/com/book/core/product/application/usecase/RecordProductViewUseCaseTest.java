package com.book.core.product.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.book.core.product.application.port.ProductViewRepositoryPort;
import com.book.core.product.domain.ProductView;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RecordProductViewUseCaseTest {
    private final ProductViewRepositoryPort repository = mock(ProductViewRepositoryPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-26T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final RecordProductViewUseCase useCase = new RecordProductViewUseCase(repository, clock);

    @Test
    void 인증_회원과_상품과_서비스_시각으로_조회_기록을_저장한다() {
        useCase.execute(42L, 20L);

        final var captor = ArgumentCaptor.forClass(ProductView.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(42L);
        assertThat(captor.getValue().productId()).isEqualTo(20L);
        assertThat(captor.getValue().viewedAt()).isEqualTo(LocalDateTime.of(2026, 10, 26, 12, 0));
    }
}
