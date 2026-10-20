package com.book.common.paging;

import java.util.List;

public record Page<T>(List<T> content, boolean hasNext) {
    public Page {
        content = List.copyOf(content);
    }
}
