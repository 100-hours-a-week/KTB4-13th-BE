package com.book.core.point.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.point.application.command.DeductPointCommand;
import com.book.core.point.application.command.EarnPointCommand;
import com.book.core.point.application.command.GetPointCommand;
import com.book.core.point.application.result.GetPointResult;
import com.book.core.point.application.usecase.DeductPointUseCase;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.application.usecase.FindUserIdsWithoutPointBalanceUseCase;
import com.book.core.point.application.usecase.GetPointUseCase;
import com.book.core.point.application.usecase.RecoverPointBalanceUseCase;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;

class PointServiceTest {
    @Test
    void 조회_Command와_적립_차감_인자를_해당_UseCase에_전달한다() {
        final GetPointUseCase get = mock(GetPointUseCase.class);
        final EarnPointUseCase earn = mock(EarnPointUseCase.class);
        final DeductPointUseCase deduct = mock(DeductPointUseCase.class);
        final PointService service =
            new PointService(get, earn, deduct, mock(FindUserIdsWithoutPointBalanceUseCase.class), mock(RecoverPointBalanceUseCase.class));
        final GetPointCommand getCommand = new GetPointCommand(42L);
        final EarnPointCommand earnCommand = new EarnPointCommand(42L, PointType.REVIEW, 10L, BigDecimal.TEN);
        final DeductPointCommand deductCommand = new DeductPointCommand(42L, PointType.REVIEW, 10L, BigDecimal.TEN);
        final GetPointResult result = new GetPointResult(42L, BigDecimal.ZERO, List.of());
        when(get.execute(getCommand)).thenReturn(result);

        assertThat(service.getPoint(getCommand)).isSameAs(result);
        service.earn(earnCommand);
        service.deduct(deductCommand);

        verify(get).execute(getCommand);
        verify(earn).execute(42L, PointType.REVIEW, 10L, BigDecimal.TEN);
        verify(deduct).execute(42L, PointType.REVIEW, 10L, BigDecimal.TEN);
    }

    @Test
    void 한_회원의_포인트_잔액_복구가_실패해도_다음_회원을_복구한다() {
        final FindUserIdsWithoutPointBalanceUseCase find = mock(FindUserIdsWithoutPointBalanceUseCase.class);
        final RecoverPointBalanceUseCase recover = mock(RecoverPointBalanceUseCase.class);
        final PointService service =
            new PointService(mock(GetPointUseCase.class), mock(EarnPointUseCase.class), mock(DeductPointUseCase.class), find, recover);
        final LocalDateTime registeredBefore = LocalDateTime.of(2030, 1, 1, 0, 0);
        when(find.execute(registeredBefore, 0L, 500)).thenReturn(List.of(1L, 2L));
        when(recover.execute(1L)).thenThrow(new DataAccessResourceFailureException("point"));

        service.recoverMissingPointBalances(registeredBefore, 500);

        verify(recover).execute(2L);
    }
}
