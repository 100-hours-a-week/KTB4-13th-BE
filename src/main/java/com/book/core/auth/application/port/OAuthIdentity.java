package com.book.core.auth.application.port;

public record OAuthIdentity(String providerUserId, String providerEmail) {}
