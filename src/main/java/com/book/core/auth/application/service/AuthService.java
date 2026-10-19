package com.book.core.auth.application.service;

import com.book.core.auth.application.command.AuthLoginCommand;
import com.book.core.auth.application.command.AuthReissueCommand;
import com.book.core.auth.application.result.AuthLoginResult;
import com.book.core.auth.application.result.AuthReissueResult;
import com.book.core.auth.application.result.OAuthUserInfo;
import com.book.core.auth.application.usecase.AuthLogoutUseCase;
import com.book.core.auth.application.usecase.AuthReissueUseCase;
import com.book.core.auth.application.usecase.AuthenticateOAuthUserUseCase;
import com.book.core.auth.application.usecase.CreateLoginSessionUseCase;
import com.book.core.user.application.command.UserRegistrationCommand;
import com.book.core.user.application.usecase.FindOAuthUserUseCase;
import com.book.core.user.application.usecase.UserRegistrationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticateOAuthUserUseCase authenticateOAuthUserUseCase;
    private final FindOAuthUserUseCase findOAuthUserUseCase;
    private final UserRegistrationUseCase userRegistrationUseCase;
    private final CreateLoginSessionUseCase createLoginSessionUseCase;
    private final AuthReissueUseCase authReissueUseCase;
    private final AuthLogoutUseCase authLogoutUseCase;

    public AuthLoginResult login(final AuthLoginCommand command) {
        final OAuthUserInfo identity =
            authenticateOAuthUserUseCase.execute(command.authorizationCode(), command.codeVerifier(), command.nonce());
        final Long userId =
            findOAuthUserUseCase.execute(command.providerType(), identity.providerUserId()).orElseGet(() -> userRegistrationUseCase
                .execute(new UserRegistrationCommand(command.providerType(), identity.providerUserId(), identity.providerEmail())));
        return createLoginSessionUseCase.execute(userId);
    }

    public AuthReissueResult reissue(final AuthReissueCommand command) {
        return authReissueUseCase.execute(command.refreshToken());
    }

    public void logout(final Long userId) {
        authLogoutUseCase.execute(userId);
    }

}
