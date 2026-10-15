package com.book.core.user.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.config.security.UserIdMvcConfig;
import com.book.core.auth.api.cookie.RefreshTokenCookieFactory;
import com.book.core.user.api.converter.UserCommandConverter;
import com.book.core.user.application.result.GetMyInfoResult;
import com.book.core.user.application.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import({UserCommandConverter.class, UserIdMvcConfig.class})
@ActiveProfiles("test")
class GetMyInfoControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserService userService;

    @MockitoBean
    RefreshTokenCookieFactory refreshTokenCookieFactory;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 인증_회원의_닉네임만_응답한다() throws Exception {
        final Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "none").claim("sub", "42").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        when(userService.getMyInfo(42L)).thenReturn(new GetMyInfoResult("책읽는북적이"));

        mvc.perform(get("/api/v1/users/me")).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.nickname").value("책읽는북적이")).andExpect(jsonPath("$.data.length()").value(1));

        verify(userService).getMyInfo(42L);
    }
}
