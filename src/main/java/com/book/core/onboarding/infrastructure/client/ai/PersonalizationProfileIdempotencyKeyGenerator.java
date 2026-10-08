package com.book.core.onboarding.infrastructure.client.ai;

import com.book.core.onboarding.application.port.AiPersonalizationProfileRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

@Component
class PersonalizationProfileIdempotencyKeyGenerator {
    private static final String ALGORITHM = "SHA-256";
    private static final String ITEM_SEPARATOR = "\u001F";
    private static final String FIELD_SEPARATOR = "\u001E";

    String generate(final AiPersonalizationProfileRequest request) {
        final String payload =
            Stream.<List<?>>of(request.readingTimes(), request.criteria(), request.categories(), request.tags(), request.likedBookIds())
                .map(values -> values.stream().map(String::valueOf).collect(Collectors.joining(ITEM_SEPARATOR)))
                .collect(Collectors.joining(FIELD_SEPARATOR));
        try {
            final MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            return "profile:" + request.userId() + ":" + HexFormat.of().formatHex(digest.digest(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 hash algorithm is unavailable.", exception);
        }
    }
}
