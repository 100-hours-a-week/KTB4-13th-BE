package com.book.core.settlement.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.settlement.application.port.SettlementRepositoryPort;
import com.book.core.settlement.application.port.SettlementTargetRepositoryPort;
import com.book.core.settlement.domain.Settlement;
import com.book.core.settlement.domain.SettlementState;
import com.book.core.settlement.domain.SettlementTargetSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SettlementUseCasesTest {
    private final SettlementRepositoryPort repository = mock(SettlementRepositoryPort.class);
    private final LocalDate date = LocalDate.of(2026, 10, 16);

    @Test
    void 생성은_가맹점별_계산_결과와_READY를_저장한다() {
        final var summary = new SettlementTargetSummary(1L, date, new BigDecimal("1000"), 2, 1);
        assertThat(new GenerateSettlementsUseCase(repository).execute(List.of(summary), Map.of(1L, BigDecimal.ZERO))).isEqualTo(1);
        final ArgumentCaptor<List<Settlement>> captured = ArgumentCaptor.captor();
        verify(repository).saveAll(captured.capture());
        final var settlement = captured.getValue().getFirst();
        assertThat(settlement.merchantId()).isEqualTo(1L);
        assertThat(settlement.settlementDate()).isEqualTo(date);
        assertThat(settlement.originalAmount()).isEqualByComparingTo("1000");
        assertThat(settlement.feeRate()).isEqualByComparingTo("0.1");
        assertThat(settlement.feeAmount()).isEqualByComparingTo("100");
        assertThat(settlement.settlementAmount()).isEqualByComparingTo("900");
        assertThat(settlement.state()).isEqualTo(SettlementState.READY);
    }

    @Test
    void 최근_SENT_매출이_없는_가맹점의_생성실패를_임의로_영원_처리하지_않는다() {
        final var summary = new SettlementTargetSummary(1L, date, BigDecimal.TEN, 1, 1);
        assertThatThrownBy(() -> new GenerateSettlementsUseCase(repository).execute(List.of(summary), Map.of()))
            .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void 빈_집계의_생성은_영건이다() {
        assertThat(new GenerateSettlementsUseCase(repository).execute(List.of(), Map.of())).isZero();
        verify(repository).saveAll(List.of());
    }

    @Test
    void 최근매출은_한달전부터_어제까지_SENT_집계_계약을_사용한다() {
        final var amounts = Map.of(1L, BigDecimal.TEN);
        when(repository.readRecentSalesAmounts(date.minusMonths(1), date.minusDays(1), List.of(1L))).thenReturn(amounts);
        assertThat(new GetSettlementsUseCase(repository).recentSalesAmounts(date, List.of(1L))).isSameAs(amounts);
        when(repository.findByState(SettlementState.READY)).thenReturn(List.of());
        assertThat(new GetSettlementsUseCase(repository).byState(SettlementState.READY)).isEmpty();
    }


    @Test
    void 대상조회는_기준일을_그대로_전달한다() {
        final var targets = mock(SettlementTargetRepositoryPort.class);
        final var summaries = List.of(new SettlementTargetSummary(1L, date, BigDecimal.TEN, 1, 1));
        when(targets.findSummary(date)).thenReturn(summaries);
        assertThat(new GetSettlementTargetsUseCase(targets).execute(date)).isSameAs(summaries);
    }
}
