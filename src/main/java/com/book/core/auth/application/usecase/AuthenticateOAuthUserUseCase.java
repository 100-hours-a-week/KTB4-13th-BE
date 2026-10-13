package com.book.core.auth.application.usecase;

import com.book.core.auth.application.port.OAuthIdentity;
import com.book.core.auth.application.port.OAuthProviderClient;
import com.book.core.auth.application.port.OAuthTokenClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticateOAuthUserUseCase {
    private final OAuthTokenClient oAuthTokenClient;
    private final OAuthProviderClient oAuthProviderClient;

    public OAuthIdentity execute(final String authorizationCode, final String codeVerifier, final String nonce) {
        final String idToken = oAuthTokenClient.exchangeForIdToken(authorizationCode, codeVerifier);
        return oAuthProviderClient.verify(idToken, nonce);
    }
}
