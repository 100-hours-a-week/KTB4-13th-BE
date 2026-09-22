package com.book.core.auth.infrastructure.token;

import com.book.core.auth.application.port.RefreshTokenHasher;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
class Sha256RefreshTokenHasher implements RefreshTokenHasher {
    private static final String ALGORITHM = "SHA-256";

    @Override
    public String hash(final String refreshToken) {
        try {
            final MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            return HexFormat.of().formatHex(digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 hash algorithm is unavailable.", exception);
        }
    }
}
