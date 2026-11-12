package com.book.core.coupon.application.port;

import com.book.core.coupon.domain.IssuanceDltLog;
import java.util.List;

public interface IssuanceDltLogRepositoryPort {
    void record(IssuanceDltLog log);

    List<ReplayRequest> lockPending(List<Long> ids);

    void markReplayed(long id);

    record ReplayRequest(long id, String messageKey, String payload) {
    }
}
