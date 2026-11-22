package com.book.core.book.infrastructure.client;

import com.book.core.book.application.port.NewBookCollectorException;
import com.book.core.book.application.port.NewBookCollectorFailureReason;
import java.lang.reflect.Method;
import org.springframework.resilience.retry.MethodRetryPredicate;

class TemporaryFailureRetryPredicate implements MethodRetryPredicate {
    @Override
    public boolean shouldRetry(final Method method, final Throwable throwable) {
        return throwable instanceof NewBookCollectorException exception && exception.reason() == NewBookCollectorFailureReason.TEMPORARY;
    }
}
