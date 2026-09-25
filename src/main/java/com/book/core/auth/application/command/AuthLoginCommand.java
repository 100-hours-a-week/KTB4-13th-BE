package com.book.core.auth.application.command;

import com.book.core.user.domain.ProviderType;

public record AuthLoginCommand(
        ProviderType providerType, String authorizationCode, String codeVerifier, String nonce) {}
