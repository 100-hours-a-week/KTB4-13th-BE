package com.book.core.auth.api.converter;

import com.book.core.auth.api.request.AuthLoginRequest;
import com.book.core.auth.application.command.AuthLoginCommand;
import com.book.core.user.domain.ProviderType;
import org.springframework.stereotype.Component;

@Component
public class AuthCommandConverter {
    public AuthLoginCommand toAuthLoginCommand(final ProviderType providerType, final AuthLoginRequest request) {
        return new AuthLoginCommand(providerType, request.authorizationCode(), request.codeVerifier(), request.nonce());
    }
}
