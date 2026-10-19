package com.book.core.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.auth.application.result.OAuthUserInfo;
import com.book.core.auth.application.port.OAuthProviderClient;
import com.book.core.auth.application.port.OAuthTokenClient;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class UserWithdrawalIntegrationTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    OAuthProviderClient oAuthProviderClient;

    @MockitoBean
    OAuthTokenClient oAuthTokenClient;

    @Test
    void 탈퇴_신청_회원은_기존_Token으로_접근할_수_없고_7일_이내_로그인하면_기존_회원으로_복구된다() throws Exception {
        final MvcResult login = signup("recover-provider").andExpect(status().isOk()).andReturn();
        final String accessToken = accessToken(login);
        final Cookie refreshToken = login.getResponse().getCookie("refreshToken");
        final Long userId = userIdOf("recover-provider");

        mvc.perform(delete("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)).andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true)).andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        assertThat(count("SELECT COUNT(*) FROM users WHERE id = ? AND delete_requested_at IS NOT NULL AND deleted_at IS NULL", userId))
            .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM refresh_sessions WHERE user_id = ? AND revoked_at IS NULL", userId)).isZero();
        mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("E401"));
        mvc.perform(post("/api/v1/auth/reissue").cookie(refreshToken)).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));

        final String recoveredAccessToken = accessToken(login("recover-provider").andExpect(status().isOk()).andReturn());

        assertThat(userIdOf("recover-provider")).isEqualTo(userId);
        assertThat(count("SELECT COUNT(*) FROM users WHERE id = ? AND delete_requested_at IS NULL", userId)).isEqualTo(1);
        mvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + recoveredAccessToken)).andExpect(status().isOk());
    }

    @Test
    void 탈퇴_신청_7일이_지나면_로그인으로_복구할_수_없다() throws Exception {
        final String accessToken = accessToken(signup("expired-provider").andExpect(status().isOk()).andReturn());
        final Long userId = userIdOf("expired-provider");
        mvc.perform(delete("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)).andExpect(status().isOk());
        jdbc.update("UPDATE users SET delete_requested_at = ? WHERE id = ?", LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(8),
            userId);

        login("expired-provider").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("USER_WITHDRAWAL_PENDING"));

        assertThat(count("SELECT COUNT(*) FROM users WHERE id = ? AND delete_requested_at IS NOT NULL", userId)).isEqualTo(1);
    }

    private ResultActions signup(final String providerUserId) throws Exception {
        return authenticate("signup", providerUserId);
    }

    private ResultActions login(final String providerUserId) throws Exception {
        return authenticate("login", providerUserId);
    }

    private ResultActions authenticate(final String action, final String providerUserId) throws Exception {
        when(oAuthTokenClient.exchangeForIdToken(providerUserId + "-code", "code-verifier")).thenReturn(providerUserId + "-id-token");
        when(oAuthProviderClient.verify(providerUserId + "-id-token", "nonce"))
            .thenReturn(new OAuthUserInfo(providerUserId, "reader@example.com"));
        return mvc.perform(post("/api/v1/auth/kakao/" + action).contentType(MediaType.APPLICATION_JSON)
            .content("{\"authorizationCode\":\"" + providerUserId + "-code\",\"codeVerifier\":\"code-verifier\",\"nonce\":\"nonce\"}"));
    }

    private String accessToken(final MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }

    private Long userIdOf(final String providerUserId) {
        return jdbc.queryForObject("SELECT user_id FROM user_providers WHERE provider_user_id = ? AND deleted_at IS NULL", Long.class,
            providerUserId);
    }

    private Integer count(final String sql, final Object arg) {
        return jdbc.queryForObject(sql, Integer.class, arg);
    }
}
