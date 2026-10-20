package com.book.core.onboarding.application.port;

import java.math.BigDecimal;
import java.util.List;

public record AiPersonalizationProfileHistory(List<String> purchasedIsbns, List<String> libraryIsbns, List<Review> reviews) {
    public static final AiPersonalizationProfileHistory EMPTY = new AiPersonalizationProfileHistory(List.of(), List.of(), List.of());

    public record Review(String isbn, BigDecimal rating, String content) {
    }
}
