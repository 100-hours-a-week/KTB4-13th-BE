package com.book.common.response;

import java.util.List;

public record PageResponse<T>(List<T> content, boolean hasNext) {}
