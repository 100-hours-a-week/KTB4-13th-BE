package com.book.core.onboarding.application.port;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// @formatter:off
public record AiPersonalizationProfileRequest(
        Long userId,
        List<String> readingTimes,
        List<String> criteria,
        List<String> categories,
        List<String> tags,
        List<Long> likedBookIds) {
    private static final String ITEM_SEPARATOR = "\u001F";
    private static final String FIELD_SEPARATOR = "\u001E";

    public String idempotencyKey() {
        final String payload = Stream.of(readingTimes, criteria, categories, tags, likedBookIds)
                .map(values -> values.stream().map(String::valueOf).collect(Collectors.joining(ITEM_SEPARATOR)))
                .collect(Collectors.joining(FIELD_SEPARATOR));
        return "profile:" + userId + ":" + sha256Hex(payload);
    }

    private static String sha256Hex(final String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 hash algorithm is unavailable.", exception);
        }
    }
}
// @formatter:on
