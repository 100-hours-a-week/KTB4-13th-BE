package com.book.core.auth.api.request;

import com.book.core.auth.application.command.AuthLoginCommand;
import com.book.core.user.domain.ProviderType;
import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequest(
        @NotBlank String authorizationCode,
        @NotBlank String codeVerifier,
        @NotBlank String nonce) {
    public AuthLoginCommand toCommand(final ProviderType providerType) {
        return new AuthLoginCommand(providerType, authorizationCode, codeVerifier, nonce);
    }
}
