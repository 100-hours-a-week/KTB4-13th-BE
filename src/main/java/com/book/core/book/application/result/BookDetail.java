package com.book.core.book.application.result;

import java.time.LocalDate;

public record BookDetail(String title, String author, String description, String publisher, String category, LocalDate publishedAt,
    String coverImageUrl) {
}
