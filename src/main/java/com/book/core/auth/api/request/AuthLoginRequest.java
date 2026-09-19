package com.book.core.auth.api.request;

import com.book.core.auth.application.command.AuthLoginCommand;
import com.book.core.user.domain.ProviderType;
import jakarta.validation.constraints.NotBlank;

public record AuthLoginRequest(@NotBlank String idToken) {
    public AuthLoginCommand toCommand(final ProviderType providerType) {
        return new AuthLoginCommand(providerType, idToken);
    }
}
