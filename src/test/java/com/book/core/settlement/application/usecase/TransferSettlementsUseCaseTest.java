package com.book.core.settlement.application.usecase;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.core.merchant.domain.Merchant;
import com.book.core.settlement.domain.Settlement;
import com.book.core.settlement.domain.SettlementState;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransferSettlementsUseCaseTest {
    private final CompleteSettlementTransferUseCase complete = mock(CompleteSettlementTransferUseCase.class);
    private final TransferSettlementsUseCase processor = new TransferSettlementsUseCase(complete);
    private final LocalDate date = LocalDate.of(2026, 10, 16);

    @Test
    void 날짜의_나머지가_영인_가맹점의_READY_전체를_합산하여_전달한다() {
        final var positive = settlement(1L, "100");
        final var negative = settlement(1L, "-20");
        processor.transfer(date, List.of(merchant(1L, 8)), List.of(positive, negative));
        verify(complete).execute(List.of(positive, negative));
    }

    @Test
    void 가맹점없음_주기아님_영원_음수는_전환하지_않는다() {
        processor.transfer(date, List.of(merchant(2L, 3), merchant(3L, 1), merchant(4L, 1)),
            List.of(settlement(1L, "100"), settlement(2L, "100"), settlement(3L, "0"), settlement(4L, "-1")));
        verifyNoInteractions(complete);
    }

    @Test
    void 한_가맹점의_주기오류_또는_저장실패가_다른_가맹점을_막지_않는다() {
        final var valid = settlement(2L, "100");
        processor.transfer(date, List.of(merchant(1L, 0), merchant(2L, 1)), List.of(settlement(1L, "100"), valid));
        verify(complete).execute(List.of(valid));
    }

    @Test
    void 한_가맹점의_상태전환_실패_후에도_다른_가맹점을_처리한다() {
        final var first = settlement(1L, "100");
        final var second = settlement(2L, "100");
        doThrow(new IllegalStateException("test")).when(complete).execute(List.of(first));
        processor.transfer(date, List.of(merchant(1L, 1), merchant(2L, 1)), List.of(first, second));
        verify(complete).execute(List.of(first));
        verify(complete).execute(List.of(second));
    }

    private Merchant merchant(final Long id, final int cycle) {
        final var merchant = mock(Merchant.class);
        when(merchant.id()).thenReturn(id);
        when(merchant.settlementCycle()).thenReturn(cycle);
        return merchant;
    }

    private Settlement settlement(final Long merchantId, final String amount) {
        return new Settlement(merchantId, date, new BigDecimal(amount), BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal(amount),
            SettlementState.READY);
    }
}
