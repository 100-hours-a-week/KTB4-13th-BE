package com.book.core.point.application.service;

import com.book.core.point.application.command.EarnPointCommand;
import com.book.core.point.application.command.GetPointCommand;
import com.book.core.point.application.result.GetPointResult;
import com.book.core.point.application.usecase.EarnPointUseCase;
import com.book.core.point.application.usecase.GetPointUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {
    private final GetPointUseCase getPointUseCase;
    private final EarnPointUseCase earnPointUseCase;

    public GetPointResult getPoint(final GetPointCommand command) {
        return getPointUseCase.execute(command);
    }

    public void earn(final EarnPointCommand command) {
        earnPointUseCase.execute(command);
    }
}
