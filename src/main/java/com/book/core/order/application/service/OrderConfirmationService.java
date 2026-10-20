package com.book.core.order.application.service;

import com.book.core.order.application.usecase.AutoConfirmOrdersUseCase;
import com.book.core.order.application.usecase.NotifyOrdersForConfirmationUseCase;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderConfirmationService {
    private final NotifyOrdersForConfirmationUseCase notifyOrdersForConfirmationUseCase;
    private final AutoConfirmOrdersUseCase autoConfirmOrdersUseCase;
    private final Clock clock;

    public void processDueConfirmations() {
        final LocalDateTime now = LocalDateTime.now(clock);
        notifyOrdersForConfirmationUseCase.execute(now);
        autoConfirmOrdersUseCase.execute(now);
    }
}
