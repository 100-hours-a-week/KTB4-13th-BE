package com.book.core.auth.application.port;

public interface TokenIssuer {
    IssuedTokens issue(final Long userId);
}
