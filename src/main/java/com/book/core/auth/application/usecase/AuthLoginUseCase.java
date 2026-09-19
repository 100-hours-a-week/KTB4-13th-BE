package com.book.core.auth.application.usecase;

import com.book.core.auth.application.command.AuthLoginCommand;
import com.book.core.auth.application.port.IssuedTokens;
import com.book.core.auth.application.port.OAuthIdentity;
import com.book.core.auth.application.port.OAuthProviderClient;
import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.auth.application.result.AuthLoginResult;
import com.book.core.user.application.command.UserResolveCommand;
import com.book.core.user.application.result.UserResolveResult;
import com.book.core.user.application.usecase.UserResolveUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthLoginUseCase {
    private final OAuthProviderClient oAuthProviderClient;
    private final UserResolveUseCase userResolveUseCase;
    private final TokenIssuer tokenIssuer;

    public AuthLoginResult execute(final AuthLoginCommand command) {
        final OAuthIdentity identity = oAuthProviderClient.verify(command.providerType(), command.idToken());
        final UserResolveCommand userResolveCommand =
                new UserResolveCommand(command.providerType(), identity.providerUserId(), identity.providerEmail());
        final UserResolveResult user = userResolveUseCase.execute(userResolveCommand);
        final IssuedTokens tokens = tokenIssuer.issue(user.userId());
        return new AuthLoginResult(tokens.accessToken(), tokens.refreshToken());
    }
}
