package com.book.core.book.application.result;

import java.time.LocalDate;

public record NewBookCandidate(String isbn13, LocalDate publishedAt) {
}
