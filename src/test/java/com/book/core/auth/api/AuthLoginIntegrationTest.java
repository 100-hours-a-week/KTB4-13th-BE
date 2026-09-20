package com.book.core.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.auth.application.port.OAuthIdentity;
import com.book.core.auth.application.port.OAuthProviderClient;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.user.domain.ProviderType;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthLoginIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    MockMvc mvc;

    @Autowired
    RefreshTokenHasher refreshTokenHasher;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    @Qualifier("tokenSecretKey")
    SecretKey secretKey;

    @MockitoBean
    OAuthProviderClient oAuthProviderClient;

    @Test
    void 로그인_HTTP_요청이_회원과_RefreshSession_저장과_Cookie_응답까지_연결된다() throws Exception {
        when(oAuthProviderClient.verify(ProviderType.KAKAO, "kakao-id-token"))
                .thenReturn(new OAuthIdentity("provider-123", "reader@example.com"));

        final MvcResult firstLogin = login();
        final String firstRefreshToken =
                firstLogin.getResponse().getCookie("refreshToken").getValue();

        final MvcResult secondLogin = login();
        final String secondRefreshToken =
                secondLogin.getResponse().getCookie("refreshToken").getValue();
        final String secondTokenHash = refreshTokenHasher.hash(secondRefreshToken);
        final Jwt decodedRefreshToken = refreshDecoder().decode(secondRefreshToken);

        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_providers", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM refresh_sessions", Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM refresh_sessions WHERE revoked_at IS NOT NULL", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM refresh_sessions WHERE active_flag = 1", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT token_hash FROM refresh_sessions WHERE active_flag = 1", String.class))
                .isEqualTo(secondTokenHash)
                .isNotEqualTo(secondRefreshToken);
        assertThat(jdbc.queryForObject(
                        "SELECT expires_at FROM refresh_sessions WHERE active_flag = 1", LocalDateTime.class))
                .isEqualTo(LocalDateTime.ofInstant(decodedRefreshToken.getExpiresAt(), ZoneOffset.UTC));
    }

    private MvcResult login() throws Exception {
        final MvcResult result = mvc.perform(post("/api/v1/auth/kakao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"kakao-id-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andReturn();
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains("Path=/api/v1/auth")
                .contains("Max-Age=604800")
                .contains("HttpOnly")
                .contains("SameSite=Lax")
                .doesNotContain("Secure");
        return result;
    }

    private NimbusJwtDecoder refreshDecoder() {
        final NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS512)
                .build();
        decoder.setJwtValidator((final Jwt jwt) -> OAuth2TokenValidatorResult.success());
        return decoder;
    }
}
