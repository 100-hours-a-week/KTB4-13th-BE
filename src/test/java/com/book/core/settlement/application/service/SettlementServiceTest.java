package com.book.core.settlement.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.merchant.application.usecase.GetMerchantsUseCase;
import com.book.core.settlement.application.usecase.GenerateSettlementsUseCase;
import com.book.core.settlement.application.usecase.GetSettlementTargetsUseCase;
import com.book.core.settlement.application.usecase.GetSettlementsUseCase;
import com.book.core.settlement.application.usecase.LoadSettlementTargetsUseCase;
import com.book.core.settlement.application.usecase.TransferSettlementsUseCase;
import com.book.core.settlement.domain.Settlement;
import com.book.core.settlement.domain.SettlementState;
import com.book.core.settlement.domain.SettlementTargetSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class SettlementServiceTest {
    private final LoadSettlementTargetsUseCase load = mock(LoadSettlementTargetsUseCase.class);
    private final GetSettlementTargetsUseCase targets = mock(GetSettlementTargetsUseCase.class);
    private final GetSettlementsUseCase settlements = mock(GetSettlementsUseCase.class);
    private final GenerateSettlementsUseCase generate = mock(GenerateSettlementsUseCase.class);
    private final GetMerchantsUseCase merchants = mock(GetMerchantsUseCase.class);
    private final TransferSettlementsUseCase transfer = mock(TransferSettlementsUseCase.class);
    private final SettlementService service = new SettlementService(load, targets, settlements, generate, merchants, transfer);
    private final LocalDate date = LocalDate.of(2026, 10, 16);

    @Test
    void 정산_대상_적재를_전용_유즈케이스에_위임한다() {
        final LocalDateTime from = date.minusDays(1).atStartOfDay();
        final LocalDateTime to = date.atStartOfDay().minusNanos(1);
        service.loadTargets(date, from, to);
        verify(load).execute(date, from, to);
    }

    @Test
    void 생성은_집계_매출조회_생성_순서를_유지한다() {
        final List<SettlementTargetSummary> summaries = List.of(new SettlementTargetSummary(1L, date, BigDecimal.TEN, 1, 1));
        final Map<Long, BigDecimal> amounts = Map.of(1L, BigDecimal.ZERO);
        when(targets.execute(date)).thenReturn(summaries);
        when(settlements.recentSalesAmounts(date, List.of(1L))).thenReturn(amounts);
        when(generate.execute(summaries, amounts)).thenReturn(1);
        assertThat(service.generate(date)).isEqualTo(1);
        final InOrder order = inOrder(targets, settlements, generate);
        order.verify(targets).execute(date);
        order.verify(settlements).recentSalesAmounts(date, List.of(1L));
        order.verify(generate).execute(summaries, amounts);
    }

    @Test
    void 이체는_READY_전체와_해당가맹점을_전달한다() {
        final List<Settlement> ready = List.of(
            new Settlement(1L, date, BigDecimal.TEN, BigDecimal.ONE, new BigDecimal("0.1"), new BigDecimal("9"), SettlementState.READY));
        when(settlements.byState(SettlementState.READY)).thenReturn(ready);
        when(merchants.execute(List.of(1L))).thenReturn(List.of());
        service.transfer(date);
        verify(transfer).transfer(date, List.of(), ready);
    }
}
