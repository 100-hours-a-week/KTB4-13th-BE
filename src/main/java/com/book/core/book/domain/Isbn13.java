package com.book.core.book.domain;

import java.util.Optional;

public final class Isbn13 {
    private Isbn13() {}

    public static Optional<String> normalize(final String value) {
        if (value == null) {
            return Optional.empty();
        }
        final String isbn13 = value.replace("-", "").replace(" ", "");
        if (!isbn13.matches("97[89][0-9]{10}")) {
            return Optional.empty();
        }
        int sum = 0;
        for (int index = 0; index < isbn13.length(); index++) {
            final int digit = isbn13.charAt(index) - '0';
            if (index % 2 == 0) {
                sum += digit;
            } else {
                sum += digit * 3;
            }
        }
        if (sum % 10 == 0) {
            return Optional.of(isbn13);
        }
        return Optional.empty();
    }
}
