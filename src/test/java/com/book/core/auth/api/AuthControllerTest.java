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
import com.book.core.auth.application.usecase.AuthLoginUseCase;
import com.book.core.user.domain.ProviderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ProviderTypePathConverter.class, RefreshTokenCookieFactory.class})
@EnableConfigurationProperties(AuthCookieProperties.class)
@TestPropertySource(properties = "auth.cookie.secure=false")
class AuthControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    AuthLoginUseCase authLoginUseCase;

    @Test
    void kakao_ID_Token으로_로그인하고_Access_Token과_Refresh_Cookie를_응답한다() throws Exception {
        when(authLoginUseCase.execute(new AuthLoginCommand(ProviderType.KAKAO, "kakao-id-token")))
                .thenReturn(new AuthLoginResult("access-token", "refresh-token"));

        final var response = mvc.perform(post("/api/v1/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"kakao-id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andReturn()
                .getResponse();

        verify(authLoginUseCase).execute(new AuthLoginCommand(ProviderType.KAKAO, "kakao-id-token"));
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
    @ValueSource(strings = {"{}", "{\"idToken\":null}", "{\"idToken\":\"\"}", "{\"idToken\":\" \"}"})
    void idToken이_누락_null_blank이면_400을_응답한다(final String body) throws Exception {
        mvc.perform(post("/api/v1/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(authLoginUseCase);
    }

    @Test
    void 지원하지_않는_provider는_400을_응답한다() throws Exception {
        mvc.perform(post("/api/v1/auth/google/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"id-token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(authLoginUseCase);
    }
}
