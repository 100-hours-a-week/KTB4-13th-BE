package com.book.core.auth.application.port;

import java.time.Instant;

public record IssuedTokens(String accessToken, String refreshToken, Instant refreshExpiresAt) {}
