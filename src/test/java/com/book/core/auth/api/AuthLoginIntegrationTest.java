package com.book.core.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.auth.application.port.OAuthIdentity;
import com.book.core.auth.application.port.OAuthProviderClient;
import com.book.core.auth.application.port.OAuthTokenClient;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.domain.Cart;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.domain.PointBalance;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
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

    @MockitoBean
    OAuthTokenClient oAuthTokenClient;

    @MockitoSpyBean
    CartRepositoryPort cartRepository;

    @MockitoSpyBean
    PointBalanceRepositoryPort pointBalanceRepository;

    @Test
    void 로그인_HTTP_요청이_회원과_RefreshSession_저장과_Cookie_응답까지_연결된다() throws Exception {
        givenKakaoUser("provider-123");

        final MvcResult firstLogin = login("provider-123");
        final String firstRefreshToken = firstLogin.getResponse().getCookie("refreshToken").getValue();

        final MvcResult secondLogin = login("provider-123");
        final String secondRefreshToken = secondLogin.getResponse().getCookie("refreshToken").getValue();
        final String secondTokenHash = refreshTokenHasher.hash(secondRefreshToken);
        final Jwt decodedRefreshToken = refreshDecoder().decode(secondRefreshToken);

        final Long userId = userIdOf("provider-123");
        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);
        assertThat(count("SELECT COUNT(*) FROM user_providers WHERE provider_user_id = ?", "provider-123")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM carts WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM point_balance WHERE user_id = ? AND balance = 0", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM refresh_sessions WHERE user_id = ?", userId)).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM refresh_sessions WHERE user_id = ? AND revoked_at IS NOT NULL", userId)).isEqualTo(1);
        assertThat(
            jdbc.queryForObject("SELECT token_hash FROM refresh_sessions WHERE user_id = ? AND active_flag = 1", String.class, userId))
            .isEqualTo(secondTokenHash).isNotEqualTo(secondRefreshToken);
        assertThat(jdbc.queryForObject("SELECT expires_at FROM refresh_sessions WHERE user_id = ? AND active_flag = 1", LocalDateTime.class,
            userId)).isEqualTo(LocalDateTime.ofInstant(decodedRefreshToken.getExpiresAt(), ZoneOffset.UTC));
    }

    @ParameterizedTest
    @CsvSource({"cart-failure, true, false", "point-failure, false, true", "both-failure, true, true"})
    void 신규_회원_초기화가_실패해도_회원을_유지하고_로그인한다(final String providerUserId, final boolean cartFails, final boolean pointFails) throws Exception {
        givenKakaoUser(providerUserId);
        if (cartFails) {
            doThrow(new DataAccessResourceFailureException("cart")).when(cartRepository).save(any(Cart.class));
        }
        if (pointFails) {
            doThrow(new DataAccessResourceFailureException("point")).when(pointBalanceRepository).save(any(PointBalance.class));
        }

        login(providerUserId);

        final Long userId = userIdOf(providerUserId);
        verify(cartRepository).save(any(Cart.class));
        verify(pointBalanceRepository).save(any(PointBalance.class));
        assertThat(count("SELECT COUNT(*) FROM users WHERE id = ? AND deleted_at IS NULL", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM carts WHERE user_id = ?", userId)).isEqualTo(cartFails ? 0 : 1);
        assertThat(count("SELECT COUNT(*) FROM point_balance WHERE user_id = ?", userId)).isEqualTo(pointFails ? 0 : 1);
        assertThat(count("SELECT COUNT(*) FROM refresh_sessions WHERE user_id = ? AND active_flag = 1", userId)).isEqualTo(1);
    }

    private void givenKakaoUser(final String providerUserId) {
        when(oAuthTokenClient.exchangeForIdToken(providerUserId + "-code", "code-verifier")).thenReturn(providerUserId + "-id-token");
        when(oAuthProviderClient.verify(providerUserId + "-id-token", "nonce")).thenReturn(new OAuthIdentity(providerUserId, null));
    }

    private MvcResult login(final String providerUserId) throws Exception {
        final MvcResult result = mvc
            .perform(post("/api/v1/auth/kakao/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"authorizationCode\":\"" + providerUserId + "-code\",\"codeVerifier\":\"code-verifier\",\"nonce\":\"nonce\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true)).andExpect(jsonPath("$.data.accessToken").isString())
            .andReturn();
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Path=/api/v1/auth").contains("HttpOnly")
            .contains("SameSite=Lax").doesNotContain("Secure");
        return result;
    }

    private Long userIdOf(final String providerUserId) {
        return jdbc.queryForObject("SELECT user_id FROM user_providers WHERE provider_user_id = ? AND deleted_at IS NULL", Long.class,
            providerUserId);
    }

    private Integer count(final String sql, final Object arg) {
        return jdbc.queryForObject(sql, Integer.class, arg);
    }

    private NimbusJwtDecoder refreshDecoder() {
        final NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS512).build();
        decoder.setJwtValidator((final Jwt jwt) -> OAuth2TokenValidatorResult.success());
        return decoder;
    }
}
