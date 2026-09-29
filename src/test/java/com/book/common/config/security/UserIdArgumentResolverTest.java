package com.book.common.config.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class UserIdArgumentResolverTest {
    private final UserIdArgumentResolver resolver = new UserIdArgumentResolver();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void supports_only_UserId_annotated_Long_parameters() throws NoSuchMethodException {
        assertThat(resolver.supportsParameter(parameter("userId", Long.class))).isTrue();
        assertThat(resolver.supportsParameter(parameter("unannotated", Long.class))).isFalse();
        assertThat(resolver.supportsParameter(parameter("wrongType", String.class))).isFalse();
    }

    @Test
    void resolves_user_ID_from_JWT_subject_not_token_ID() throws Exception {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").subject("42").claim("jti", "token-id").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        assertThat(resolver.resolveArgument(parameter("userId", Long.class), null, null, null)).isEqualTo(42L);
    }

    @Test
    void rejects_a_missing_JWT_principal() throws Exception {
        assertThatThrownBy(() -> resolver.resolveArgument(parameter("userId", Long.class), null, null, null))
            .isInstanceOf(IllegalStateException.class);
    }

    private MethodParameter parameter(final String methodName, final Class<?> parameterType) throws NoSuchMethodException {
        final Method method = ResolverMethods.class.getDeclaredMethod(methodName, parameterType);
        return new MethodParameter(method, 0);
    }

    private static class ResolverMethods {
        void userId(@UserId final Long userId) {}

        void unannotated(final Long userId) {}

        void wrongType(@UserId final String userId) {}
    }
}
