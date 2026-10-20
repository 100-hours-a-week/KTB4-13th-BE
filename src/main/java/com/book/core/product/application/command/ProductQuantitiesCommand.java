package com.book.core.product.application.command;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public record ProductQuantitiesCommand(Map<Long, Integer> quantities) {
    public ProductQuantitiesCommand {
        if (quantities == null || quantities.isEmpty()
            || quantities.entrySet().stream().anyMatch((final Map.Entry<Long, Integer> entry) -> entry.getKey() == null
                || entry.getKey() <= 0 || entry.getValue() == null || entry.getValue() <= 0)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        quantities = Collections.unmodifiableMap(new TreeMap<>(quantities));
    }
}
