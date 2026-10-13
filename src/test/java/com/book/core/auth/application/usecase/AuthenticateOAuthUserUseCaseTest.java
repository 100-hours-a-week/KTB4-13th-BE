package com.book.core.auth.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.application.port.OAuthIdentity;
import com.book.core.auth.application.port.OAuthProviderClient;
import com.book.core.auth.application.port.OAuthTokenClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticateOAuthUserUseCaseTest {
    @Mock
    OAuthTokenClient oAuthTokenClient;

    @Mock
    OAuthProviderClient oAuthProviderClient;

    @Test
    void 인가_코드를_ID_Token으로_교환하고_검증한_OAuthIdentity를_반환한다() {
        final OAuthIdentity identity = new OAuthIdentity("provider-123", null);
        when(oAuthTokenClient.exchangeForIdToken("authorization-code", "code-verifier")).thenReturn("id-token");
        when(oAuthProviderClient.verify("id-token", "nonce")).thenReturn(identity);

        final OAuthIdentity result = useCase().execute("authorization-code", "code-verifier", "nonce");

        final InOrder order = inOrder(oAuthTokenClient, oAuthProviderClient);
        order.verify(oAuthTokenClient).exchangeForIdToken("authorization-code", "code-verifier");
        order.verify(oAuthProviderClient).verify("id-token", "nonce");
        assertThat(result).isEqualTo(identity);
    }

    @Test
    void 인가_코드_교환이_실패하면_ID_Token을_검증하지_않는다() {
        final BusinessException exception = new BusinessException(ErrorCode.OAUTH_TOKEN_EXCHANGE_FAILURE);
        when(oAuthTokenClient.exchangeForIdToken("authorization-code", "code-verifier")).thenThrow(exception);

        assertThatThrownBy(() -> useCase().execute("authorization-code", "code-verifier", "nonce")).isSameAs(exception);

        verifyNoInteractions(oAuthProviderClient);
    }

    @Test
    void ID_Token_검증_실패를_그대로_전달한다() {
        final BusinessException exception = new BusinessException(ErrorCode.INVALID_ID_TOKEN);
        when(oAuthTokenClient.exchangeForIdToken("authorization-code", "code-verifier")).thenReturn("invalid-id-token");
        when(oAuthProviderClient.verify("invalid-id-token", "nonce")).thenThrow(exception);

        assertThatThrownBy(() -> useCase().execute("authorization-code", "code-verifier", "nonce")).isSameAs(exception);
    }

    private AuthenticateOAuthUserUseCase useCase() {
        return new AuthenticateOAuthUserUseCase(oAuthTokenClient, oAuthProviderClient);
    }
}
