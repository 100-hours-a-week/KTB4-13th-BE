package com.book.core.point.application.service;

import com.book.core.point.application.usecase.ExpirePointsUseCase;
import com.book.core.point.application.usecase.NotifyExpiringPointsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointExpiryService {
    private final NotifyExpiringPointsUseCase notifyExpiringPointsUseCase;
    private final ExpirePointsUseCase expirePointsUseCase;

    public void process() {
        notifyExpiringPointsUseCase.execute();
        expirePointsUseCase.execute();
    }
}
