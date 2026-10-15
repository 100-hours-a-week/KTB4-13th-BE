package com.book.core.auth.application.service;

import com.book.core.auth.application.command.AuthLoginCommand;
import com.book.core.auth.application.command.AuthReissueCommand;
import com.book.core.auth.application.port.IssuedTokens;
import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.auth.application.result.AuthLoginResult;
import com.book.core.auth.application.result.AuthReissueResult;
import com.book.core.auth.application.result.OAuthUserInfo;
import com.book.core.auth.application.usecase.AuthLogoutUseCase;
import com.book.core.auth.application.usecase.AuthReissueUseCase;
import com.book.core.auth.application.usecase.AuthenticateOAuthUserUseCase;
import com.book.core.auth.application.usecase.RefreshSessionRegistrationUseCase;
import com.book.core.user.application.command.IdentifyUserCommand;
import com.book.core.user.application.usecase.IdentifyUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticateOAuthUserUseCase authenticateOAuthUserUseCase;
    private final IdentifyUserUseCase identifyUserUseCase;
    private final TokenIssuer tokenIssuer;
    private final RefreshSessionRegistrationUseCase refreshSessionRegistrationUseCase;
    private final AuthReissueUseCase authReissueUseCase;
    private final AuthLogoutUseCase authLogoutUseCase;

    public AuthLoginResult login(final AuthLoginCommand command) {
        final OAuthUserInfo identity =
            authenticateOAuthUserUseCase.execute(command.authorizationCode(), command.codeVerifier(), command.nonce());
        final Long userId = identifyUserUseCase
            .execute(new IdentifyUserCommand(command.providerType(), identity.providerUserId(), identity.providerEmail())).userId();
        return issueAndRegisterTokens(userId);
    }

    public AuthReissueResult reissue(final AuthReissueCommand command) {
        return authReissueUseCase.execute(command.refreshToken());
    }

    public void logout(final Long userId) {
        authLogoutUseCase.execute(userId);
    }

    private AuthLoginResult issueAndRegisterTokens(final Long userId) {
        final IssuedTokens tokens = tokenIssuer.issue(userId);
        refreshSessionRegistrationUseCase.execute(userId, tokens.refreshToken(), tokens.refreshExpiresAt());
        return new AuthLoginResult(tokens.accessToken(), tokens.refreshToken(), tokens.refreshExpiresAt());
    }
}
