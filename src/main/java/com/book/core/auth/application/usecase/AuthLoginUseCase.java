package com.book.core.auth.application.usecase;

import com.book.core.auth.application.command.AuthLoginCommand;
import com.book.core.auth.application.port.IssuedTokens;
import com.book.core.auth.application.port.OAuthIdentity;
import com.book.core.auth.application.port.OAuthProviderClient;
import com.book.core.auth.application.port.OAuthTokenClient;
import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.auth.application.result.AuthLoginResult;
import com.book.core.user.application.command.IdentifyUserCommand;
import com.book.core.user.application.result.IdentifyUserResult;
import com.book.core.user.application.usecase.IdentifyUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthLoginUseCase {
    private final OAuthTokenClient oAuthTokenClient;
    private final OAuthProviderClient oAuthProviderClient;
    private final IdentifyUserUseCase identifyUserUseCase;
    private final TokenIssuer tokenIssuer;
    private final RefreshSessionRegistrationUseCase refreshSessionRegistrationUseCase;

    public AuthLoginResult execute(final AuthLoginCommand command) {
        final String idToken = oAuthTokenClient.exchangeForIdToken(
                command.providerType(), command.authorizationCode(), command.codeVerifier());
        final OAuthIdentity identity = oAuthProviderClient.verify(command.providerType(), idToken, command.nonce());
        final IdentifyUserCommand identifyUserCommand =
                new IdentifyUserCommand(command.providerType(), identity.providerUserId(), identity.providerEmail());
        final IdentifyUserResult user = identifyUserUseCase.execute(identifyUserCommand);
        final IssuedTokens tokens = tokenIssuer.issue(user.userId());
        refreshSessionRegistrationUseCase.execute(user.userId(), tokens.refreshToken(), tokens.refreshExpiresAt());
        return new AuthLoginResult(tokens.accessToken(), tokens.refreshToken());
    }
}
