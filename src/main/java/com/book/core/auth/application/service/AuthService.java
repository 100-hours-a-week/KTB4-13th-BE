package com.book.core.auth.application.service;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
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
import com.book.core.user.domain.ProviderType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticateOAuthUserUseCase authenticateOAuthUserUseCase;
    private final FindOAuthUserUseCase findOAuthUserUseCase;
    private final SignUpService signUpService;
    private final CreateLoginSessionUseCase createLoginSessionUseCase;
    private final AuthReissueUseCase authReissueUseCase;
    private final AuthLogoutUseCase authLogoutUseCase;

    public AuthLoginResult login(final AuthLoginCommand command) {
        final OAuthUserInfo identity = authenticate(command);
        final Long userId = findOAuthUserUseCase.execute(command.providerType(), identity.providerUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.SIGNUP_REQUIRED));
        return createLoginSessionUseCase.execute(userId);
    }

    public AuthLoginResult signup(final AuthLoginCommand command) {
        final OAuthUserInfo identity = authenticate(command);
        if (findOAuthUserUseCase.execute(command.providerType(), identity.providerUserId()).isPresent()) {
            throw new BusinessException(ErrorCode.ALREADY_REGISTERED);
        }
        return createLoginSessionUseCase.execute(register(command.providerType(), identity));
    }

    public AuthReissueResult reissue(final AuthReissueCommand command) {
        return authReissueUseCase.execute(command.refreshToken());
    }

    public void logout(final Long userId) {
        authLogoutUseCase.execute(userId);
    }

    private OAuthUserInfo authenticate(final AuthLoginCommand command) {
        return authenticateOAuthUserUseCase.execute(command.authorizationCode(), command.codeVerifier(), command.nonce());
    }

    private Long register(final ProviderType providerType, final OAuthUserInfo identity) {
        return signUpService.signUp(new UserRegistrationCommand(providerType, identity.providerUserId(), identity.providerEmail()));
    }
}
