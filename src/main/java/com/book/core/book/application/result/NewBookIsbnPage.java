package com.book.core.book.application.result;

import java.util.List;

public record NewBookIsbnPage(List<NewBookCandidate> candidates, boolean hasNext) {
}
