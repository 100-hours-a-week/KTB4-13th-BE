package com.book.core.product.application.port;

import java.util.List;
import java.util.Map;

public interface ProductInventoryLockPort {
    Map<Long, String> acquireAll(List<Long> productIds);

    void releaseAll(Map<Long, String> lockTokensByProductId);
}
