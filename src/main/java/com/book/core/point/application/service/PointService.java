package com.book.core.point.application.service;

import com.book.core.point.application.command.GetPointCommand;
import com.book.core.point.application.result.GetPointResult;
import com.book.core.point.application.usecase.GetPointUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {
    private final GetPointUseCase getPointUseCase;

    public GetPointResult getPoint(final GetPointCommand command) {
        return getPointUseCase.execute(command);
    }
}
