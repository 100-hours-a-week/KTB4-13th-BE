package com.book.core.auth.application.port;

public record IssuedTokens(String accessToken, String refreshToken) {}
