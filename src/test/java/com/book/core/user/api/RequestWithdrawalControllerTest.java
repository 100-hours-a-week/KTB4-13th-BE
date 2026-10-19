package com.book.core.user.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.config.security.UserIdMvcConfig;
import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.api.cookie.AuthCookieProperties;
import com.book.core.auth.api.cookie.RefreshTokenCookieFactory;
import com.book.core.user.api.converter.UserCommandConverter;
import com.book.core.user.application.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({UserCommandConverter.class, UserIdMvcConfig.class, RefreshTokenCookieFactory.class})
@EnableConfigurationProperties(AuthCookieProperties.class)
@ActiveProfiles("test")
class RequestWithdrawalControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserService userService;

    @BeforeEach
    void authenticate() {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "42").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 탈퇴_신청에_성공하면_로그아웃과_같은_Refresh_Token_쿠키_만료를_응답한다() throws Exception {
        final String setCookie = mvc.perform(delete("/api/v1/users/me")).andExpect(status().isOk())
            .andExpect(content().json("{\"success\":true}", true)).andReturn().getResponse().getHeader(HttpHeaders.SET_COOKIE);

        verify(userService).requestWithdrawal(42L);
        assertThat(setCookie).contains("refreshToken=").contains("Path=/api/v1/auth").contains("Max-Age=0").contains("HttpOnly")
            .contains("SameSite=Lax").doesNotContain("Secure");
    }

    @Test
    void 탈퇴_신청이_실패하면_실패를_응답하고_쿠키를_만료하지_않는다() throws Exception {
        doThrow(new BusinessException(ErrorCode.USER_NOT_FOUND)).when(userService).requestWithdrawal(42L);

        mvc.perform(delete("/api/v1/users/me")).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }
}
