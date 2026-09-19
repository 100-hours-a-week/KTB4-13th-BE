package com.book.core.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.common.exception.CommonErrorCode;
import com.book.core.auth.application.command.AuthLoginCommand;
import com.book.core.auth.application.port.IssuedTokens;
import com.book.core.auth.application.port.OAuthIdentity;
import com.book.core.auth.application.port.OAuthProviderClient;
import com.book.core.auth.application.port.TokenIssuer;
import com.book.core.user.application.command.UserResolveCommand;
import com.book.core.user.application.result.UserResolveResult;
import com.book.core.user.application.usecase.UserResolveUseCase;
import com.book.core.user.domain.ProviderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthLoginUseCaseTest {
    @Mock
    OAuthProviderClient oAuthProviderClient;

    @Mock
    UserResolveUseCase userResolveUseCase;

    @Mock
    TokenIssuer tokenIssuer;

    @Test
    void 검증한_OAuth_identity로_회원을_resolve하고_발급한_token을_반환한다() {
        final AuthLoginCommand command = new AuthLoginCommand(ProviderType.KAKAO, "id-token");
        when(oAuthProviderClient.verify(ProviderType.KAKAO, "id-token"))
                .thenReturn(new OAuthIdentity("provider-123", "reader@example.com"));
        when(userResolveUseCase.execute(
                        new UserResolveCommand(ProviderType.KAKAO, "provider-123", "reader@example.com")))
                .thenReturn(new UserResolveResult(42L));
        when(tokenIssuer.issue(42L)).thenReturn(new IssuedTokens("access-token", "refresh-token"));

        final var result = useCase().execute(command);

        final var resolveCommand = ArgumentCaptor.forClass(UserResolveCommand.class);
        verify(oAuthProviderClient).verify(ProviderType.KAKAO, "id-token");
        verify(userResolveUseCase).execute(resolveCommand.capture());
        assertThat(resolveCommand.getValue().providerType()).isEqualTo(ProviderType.KAKAO);
        assertThat(resolveCommand.getValue().providerUserId()).isEqualTo("provider-123");
        assertThat(resolveCommand.getValue().providerEmail()).isEqualTo("reader@example.com");
        verify(tokenIssuer).issue(42L);
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
    }

    @Test
    void nullable_providerEmail을_UserResolveCommand에_그대로_전달한다() {
        final AuthLoginCommand command = new AuthLoginCommand(ProviderType.KAKAO, "id-token");
        when(oAuthProviderClient.verify(ProviderType.KAKAO, "id-token"))
                .thenReturn(new OAuthIdentity("provider-123", null));
        when(userResolveUseCase.execute(new UserResolveCommand(ProviderType.KAKAO, "provider-123", null)))
                .thenReturn(new UserResolveResult(42L));
        when(tokenIssuer.issue(42L)).thenReturn(new IssuedTokens("access-token", "refresh-token"));

        useCase().execute(command);

        final var resolveCommand = ArgumentCaptor.forClass(UserResolveCommand.class);
        verify(userResolveUseCase).execute(resolveCommand.capture());
        assertThat(resolveCommand.getValue().providerEmail()).isNull();
    }

    @Test
    void OAuth_검증이_실패하면_회원_resolve와_token_발급을_호출하지_않는다() {
        final AuthLoginCommand command = new AuthLoginCommand(ProviderType.KAKAO, "invalid-id-token");
        final BusinessException exception = new BusinessException(CommonErrorCode.STORAGE_FAILURE);
        when(oAuthProviderClient.verify(ProviderType.KAKAO, "invalid-id-token")).thenThrow(exception);

        assertThatThrownBy(() -> useCase().execute(command)).isSameAs(exception);

        verifyNoInteractions(userResolveUseCase, tokenIssuer);
    }

    @Test
    void 회원_resolve가_실패하면_token_발급을_호출하지_않는다() {
        final AuthLoginCommand command = new AuthLoginCommand(ProviderType.KAKAO, "id-token");
        final BusinessException exception = new BusinessException(CommonErrorCode.STORAGE_FAILURE);
        when(oAuthProviderClient.verify(ProviderType.KAKAO, "id-token"))
                .thenReturn(new OAuthIdentity("provider-123", null));
        when(userResolveUseCase.execute(new UserResolveCommand(ProviderType.KAKAO, "provider-123", null)))
                .thenThrow(exception);

        assertThatThrownBy(() -> useCase().execute(command)).isSameAs(exception);

        verifyNoInteractions(tokenIssuer);
    }

    private AuthLoginUseCase useCase() {
        return new AuthLoginUseCase(oAuthProviderClient, userResolveUseCase, tokenIssuer);
    }
}
