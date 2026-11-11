package com.book.core.coupon.application.port;

import com.book.core.coupon.domain.IssuanceDltLog;
import java.util.Optional;

public interface IssuanceDltLogRepositoryPort {
    void record(IssuanceDltLog log);

    Optional<ReplayRequest> lockNextRequested();

    void markReplayed(long id);

    record ReplayRequest(long id, String messageKey, String payload) {
    }
}
