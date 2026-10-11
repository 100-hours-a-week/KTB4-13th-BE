package com.book.core.point.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.point.application.command.EarnPointCommand;
import com.book.core.point.application.command.GetPointCommand;
import com.book.core.point.application.result.GetPointResult;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.application.usecase.GetPointUseCase;
import com.book.core.point.domain.enums.PointType;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PointServiceTest {
    @Test
    void 각_Command를_해당_UseCase에_전달한다() {
        final GetPointUseCase get = mock(GetPointUseCase.class);
        final EarnPointUseCase earn = mock(EarnPointUseCase.class);
        final PointService service = new PointService(get, earn);
        final GetPointCommand getCommand = new GetPointCommand(42L);
        final EarnPointCommand earnCommand = new EarnPointCommand(42L, PointType.REVIEW, 10L, BigDecimal.TEN);
        final GetPointResult result = new GetPointResult(42L, BigDecimal.ZERO, List.of());
        when(get.execute(getCommand)).thenReturn(result);

        assertThat(service.getPoint(getCommand)).isSameAs(result);
        service.earn(earnCommand);

        verify(get).execute(getCommand);
        verify(earn).execute(earnCommand);
    }
}
