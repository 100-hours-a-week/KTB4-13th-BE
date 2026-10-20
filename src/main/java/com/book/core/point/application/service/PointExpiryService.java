package com.book.core.point.application.service;

import com.book.core.point.application.usecase.ExpirePointsUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointExpiryService {
    private final ExpirePointsUseCase expirePointsUseCase;

    public void process() {
        expirePointsUseCase.execute();
    }
}
