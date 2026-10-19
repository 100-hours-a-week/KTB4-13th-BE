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
import com.book.core.cart.application.usecase.CreateCartUseCase;
import com.book.core.point.application.usecase.CreatePointBalanceUseCase;
import com.book.core.user.application.command.UserRegistrationCommand;
import com.book.core.user.application.usecase.FindOAuthUserUseCase;
import com.book.core.user.application.usecase.UserRegistrationUseCase;
import com.book.core.user.domain.ProviderType;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticateOAuthUserUseCase authenticateOAuthUserUseCase;
    private final FindOAuthUserUseCase findOAuthUserUseCase;
    private final UserRegistrationUseCase userRegistrationUseCase;
    private final CreateCartUseCase createCartUseCase;
    private final CreatePointBalanceUseCase createPointBalanceUseCase;
    private final CreateLoginSessionUseCase createLoginSessionUseCase;
    private final AuthReissueUseCase authReissueUseCase;
    private final AuthLogoutUseCase authLogoutUseCase;

    public AuthLoginResult login(final AuthLoginCommand command) {
        final OAuthUserInfo identity =
            authenticateOAuthUserUseCase.execute(command.authorizationCode(), command.codeVerifier(), command.nonce());
        final Long userId = findOAuthUserUseCase.execute(command.providerType(), identity.providerUserId())
            .orElseGet(() -> register(command.providerType(), identity));
        return createLoginSessionUseCase.execute(userId);
    }

    public AuthReissueResult reissue(final AuthReissueCommand command) {
        return authReissueUseCase.execute(command.refreshToken());
    }

    public void logout(final Long userId) {
        authLogoutUseCase.execute(userId);
    }

    private Long register(final ProviderType providerType, final OAuthUserInfo identity) {
        final Long userId =
            userRegistrationUseCase.execute(new UserRegistrationCommand(providerType, identity.providerUserId(), identity.providerEmail()));
        initialize("장바구니", userId, createCartUseCase::execute);
        initialize("포인트 잔액", userId, createPointBalanceUseCase::execute);
        return userId;
    }

    private void initialize(final String target, final Long userId, final Consumer<Long> initializer) {
        try {
            initializer.accept(userId);
        } catch (final RuntimeException exception) {
            log.error("회원 가입 후 {} 초기화에 실패했습니다. userId={}", target, userId, exception);
        }
    }
}
