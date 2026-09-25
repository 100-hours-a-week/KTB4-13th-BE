package com.book.core.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.auth.api.cookie.AuthCookieProperties;
import com.book.core.auth.api.cookie.RefreshTokenCookieFactory;
import com.book.core.auth.application.command.AuthLoginCommand;
import com.book.core.auth.application.result.AuthLoginResult;
import com.book.core.auth.application.result.AuthReissueResult;
import com.book.core.auth.application.usecase.AuthLoginUseCase;
import com.book.core.auth.application.usecase.AuthLogoutUseCase;
import com.book.core.auth.application.usecase.AuthReissueUseCase;
import com.book.core.user.domain.ProviderType;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
    ProviderTypePathConverter.class,
    RefreshTokenCookieFactory.class,
    AuthControllerTest.AuthenticationPrincipalTestConfig.class
})
@EnableConfigurationProperties(AuthCookieProperties.class)
@TestPropertySource(properties = "auth.cookie.secure=false")
class AuthControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuthLoginUseCase authLoginUseCase;

    @MockitoBean
    AuthLogoutUseCase authLogoutUseCase;

    @MockitoBean
    AuthReissueUseCase authReissueUseCase;

    @Test
    void kakao_인가_코드로_로그인하고_Access_Token과_Refresh_Cookie를_응답한다() throws Exception {
        final AuthLoginCommand command =
                new AuthLoginCommand(ProviderType.KAKAO, "authorization-code", "code-verifier", "nonce");
        when(authLoginUseCase.execute(command)).thenReturn(new AuthLoginResult("access-token", "refresh-token"));

        final var response = mvc.perform(post("/api/v1/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andReturn()
                .getResponse();

        verify(authLoginUseCase).execute(command);
        final String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);
        assertThat(setCookie)
                .contains("refreshToken=refresh-token")
                .contains("Path=/api/v1/auth")
                .contains("Max-Age=604800")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .doesNotContain("Secure");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"codeVerifier\":\"code-verifier\",\"nonce\":\"nonce\"}",
                "{\"authorizationCode\":null,\"codeVerifier\":\"code-verifier\",\"nonce\":\"nonce\"}",
                "{\"authorizationCode\":\"\",\"codeVerifier\":\"code-verifier\",\"nonce\":\"nonce\"}",
                "{\"authorizationCode\":\" \",\"codeVerifier\":\"code-verifier\",\"nonce\":\"nonce\"}"
            })
    void authorizationCode가_누락_null_blank이면_400을_응답한다(final String body) throws Exception {
        assertInvalidRequest(body);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"authorizationCode\":\"authorization-code\",\"nonce\":\"nonce\"}",
                "{\"authorizationCode\":\"authorization-code\",\"codeVerifier\":null,\"nonce\":\"nonce\"}",
                "{\"authorizationCode\":\"authorization-code\",\"codeVerifier\":\"\",\"nonce\":\"nonce\"}",
                "{\"authorizationCode\":\"authorization-code\",\"codeVerifier\":\" \",\"nonce\":\"nonce\"}"
            })
    void codeVerifier가_누락_null_blank이면_400을_응답한다(final String body) throws Exception {
        assertInvalidRequest(body);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"authorizationCode\":\"authorization-code\",\"codeVerifier\":\"code-verifier\"}",
                "{\"authorizationCode\":\"authorization-code\",\"codeVerifier\":\"code-verifier\",\"nonce\":null}",
                "{\"authorizationCode\":\"authorization-code\",\"codeVerifier\":\"code-verifier\",\"nonce\":\"\"}",
                "{\"authorizationCode\":\"authorization-code\",\"codeVerifier\":\"code-verifier\",\"nonce\":\" \"}"
            })
    void nonce가_누락_null_blank이면_400을_응답한다(final String body) throws Exception {
        assertInvalidRequest(body);
    }

    private void assertInvalidRequest(final String body) throws Exception {
        mvc.perform(post("/api/v1/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(authLoginUseCase);
    }

    @Test
    void 지원하지_않는_provider는_400을_응답한다() throws Exception {
        mvc.perform(post("/api/v1/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginRequest()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));

        verifyNoInteractions(authLoginUseCase);
    }

    private String loginRequest() {
        return "{\"authorizationCode\":\"authorization-code\","
                + "\"codeVerifier\":\"code-verifier\",\"nonce\":\"nonce\"}";
    }

    @Test
    void Refresh_Cookie로_재발급하고_새_Access_Token과_Refresh_Cookie를_응답한다() throws Exception {
        when(authReissueUseCase.execute("old-refresh-token"))
                .thenReturn(new AuthReissueResult("new-access-token", "new-refresh-token"));

        final MockHttpServletResponse response = mvc.perform(post("/api/v1/auth/reissue")
                        .cookie(new Cookie(RefreshTokenCookieFactory.COOKIE_NAME, "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andReturn()
                .getResponse();

        verify(authReissueUseCase).execute("old-refresh-token");

        final String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);

        assertThat(setCookie)
                .contains("refreshToken=new-refresh-token")
                .contains("Path=/api/v1/auth")
                .contains("Max-Age=604800")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .doesNotContain("Secure");
    }

    @Test
    void Refresh_Cookie가_없으면_401을_응답한다() throws Exception {
        mvc.perform(post("/api/v1/auth/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        verifyNoInteractions(authReissueUseCase);
    }

    @Test
    void Access_Token으로_로그아웃하고_Refresh_Cookie를_만료한다() throws Exception {
        final Jwt jwt = Jwt.withTokenValue("access-token")
                .header("alg", "HS512")
                .subject("42")
                .claim("tokenType", "ACCESS")
                .build();

        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        try {
            final MockHttpServletResponse response = mvc.perform(post("/api/v1/auth/logout"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse();

            verify(authLogoutUseCase).execute(42L);

            assertThat(response.getContentAsString()).isEmpty();

            final String setCookie = response.getHeader(HttpHeaders.SET_COOKIE);

            assertThat(setCookie)
                    .contains("refreshToken=")
                    .contains("Path=/api/v1/auth")
                    .contains("Max-Age=0")
                    .contains("HttpOnly")
                    .contains("SameSite=Lax")
                    .doesNotContain("Secure");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @TestConfiguration
    static class AuthenticationPrincipalTestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(final List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }
}
