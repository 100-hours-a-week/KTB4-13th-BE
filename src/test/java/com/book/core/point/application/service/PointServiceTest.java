package com.book.core.point.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.book.core.point.application.command.GetPointCommand;
import com.book.core.point.application.result.GetPointResult;
import com.book.core.point.application.usecase.GetPointUseCase;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PointServiceTest {
    @Test
    void 조회_Command를_조회_UseCase에_전달한다() {
        final GetPointUseCase get = mock(GetPointUseCase.class);
        final PointService service = new PointService(get);
        final GetPointCommand command = new GetPointCommand(42L);
        final GetPointResult result = new GetPointResult(42L, BigDecimal.ZERO, List.of());
        when(get.execute(command)).thenReturn(result);

        assertThat(service.getPoint(command)).isSameAs(result);
        verify(get).execute(command);
    }
}
