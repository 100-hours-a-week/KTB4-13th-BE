package com.book.core.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.core.auth.application.port.OAuthProviderClient;
import com.book.core.auth.application.port.OAuthTokenClient;
import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.auth.application.result.OAuthUserInfo;
import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.cart.application.port.CartRepositoryPort;
import com.book.core.cart.domain.Cart;
import com.book.core.point.application.port.PointBalanceRepositoryPort;
import com.book.core.point.domain.PointBalance;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
@Testcontainers
class AuthLoginIntegrationTest {
    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(final DynamicPropertyRegistry properties) {
        properties.add("spring.data.redis.host", REDIS::getHost);
        properties.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    StringRedisTemplate redis;
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

    @MockitoSpyBean
    RefreshSessionRepository refreshSessionRepository;

    @Test
    void 가입과_로그인_HTTP_요청이_회원과_RefreshSession_저장과_Cookie_응답까지_연결된다() throws Exception {
        givenKakaoUser("provider-123");

        final MvcResult firstLogin = authenticate("signup", "provider-123");
        final String firstRefreshToken = firstLogin.getResponse().getCookie("refreshToken").getValue();

        final MvcResult secondLogin = authenticate("login", "provider-123");
        final String secondRefreshToken = secondLogin.getResponse().getCookie("refreshToken").getValue();
        final String secondTokenHash = refreshTokenHasher.hash(secondRefreshToken);
        final Jwt decodedRefreshToken = refreshDecoder().decode(secondRefreshToken);

        final Long userId = userIdOf("provider-123");
        assertThat(secondRefreshToken).isNotEqualTo(firstRefreshToken);
        assertThat(count("SELECT COUNT(*) FROM user_providers WHERE provider_user_id = ?", "provider-123")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM carts WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM point_balance WHERE user_id = ? AND balance = 0", userId)).isEqualTo(1);
        final String key = "auth:refresh-session:v1:{" + userId + "}";
        assertThat(count("SELECT COUNT(*) FROM refresh_sessions WHERE user_id = ?", userId)).isZero();
        assertThat(redis.opsForValue().get(key)).isEqualTo(secondTokenHash).isNotEqualTo(secondRefreshToken);
        assertThat(redis.getExpire(key, java.util.concurrent.TimeUnit.MILLISECONDS)).isPositive()
            .isLessThanOrEqualTo(java.time.Duration.between(java.time.Instant.now(), decodedRefreshToken.getExpiresAt()).toMillis() + 1000);
        assertThat(refreshSessionRepository.rotate(userId, refreshTokenHasher.hash(firstRefreshToken), "a".repeat(64),
            decodedRefreshToken.getExpiresAt())).isFalse();
    }

    @ParameterizedTest
    @CsvSource({"cart-failure, true", "point-failure, false"})
    void 신규_회원_초기화가_실패하면_회원_등록까지_모두_Rollback하고_가입을_실패한다(final String providerUserId, final boolean cartFails) throws Exception {
        givenKakaoUser(providerUserId);
        if (cartFails) {
            doThrow(new DataAccessResourceFailureException("cart")).when(cartRepository).save(any(Cart.class));
        } else {
            doThrow(new DataAccessResourceFailureException("point")).when(pointBalanceRepository).save(any(PointBalance.class));
        }
        final List<Integer> before = rowCounts();

        perform("signup", providerUserId).andExpect(status().isInternalServerError()).andExpect(jsonPath("$.success").value(false))
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        assertThat(count("SELECT COUNT(*) FROM user_providers WHERE provider_user_id = ?", providerUserId)).isZero();
        assertThat(rowCounts()).isEqualTo(before);
    }

    @Test
    void 미가입_회원의_로그인은_404이고_가입한_회원의_재가입은_409이다() throws Exception {
        givenKakaoUser("registration-state");

        assertError(perform("login", "registration-state"), 404, "SIGNUP_REQUIRED");
        assertThat(count("SELECT COUNT(*) FROM user_providers WHERE provider_user_id = ?", "registration-state")).isZero();

        authenticate("signup", "registration-state");
        assertError(perform("signup", "registration-state"), 409, "ALREADY_REGISTERED");
        assertThat(count("SELECT COUNT(*) FROM user_providers WHERE provider_user_id = ?", "registration-state")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM carts WHERE user_id = ?", userIdOf("registration-state"))).isEqualTo(1);
    }

    @Test
    void Session_등록이_실패하면_가입과_초기화는_남고_재가입은_409_로그인은_성공한다() throws Exception {
        givenKakaoUser("session-failure");
        doThrow(new BusinessException(ErrorCode.AUTH_SESSION_UNAVAILABLE)).when(refreshSessionRepository).replace(any(), any(), any());

        perform("signup", "session-failure").andExpect(status().isServiceUnavailable())
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        final Long userId = userIdOf("session-failure");
        assertThat(count("SELECT COUNT(*) FROM carts WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM point_balance WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM refresh_sessions WHERE user_id = ?", userId)).isZero();

        reset(refreshSessionRepository);
        assertError(perform("signup", "session-failure"), 409, "ALREADY_REGISTERED");
        authenticate("login", "session-failure");
        assertThat(redis.hasKey("auth:refresh-session:v1:{" + userId + "}")).isTrue();
    }

    private void givenKakaoUser(final String providerUserId) {
        when(oAuthTokenClient.exchangeForIdToken(providerUserId + "-code", "code-verifier")).thenReturn(providerUserId + "-id-token");
        when(oAuthProviderClient.verify(providerUserId + "-id-token", "nonce")).thenReturn(new OAuthUserInfo(providerUserId, null));
    }

    private ResultActions perform(final String action, final String providerUserId) throws Exception {
        return mvc.perform(post("/api/v1/auth/kakao/" + action).contentType(MediaType.APPLICATION_JSON)
            .content("{\"authorizationCode\":\"" + providerUserId + "-code\",\"codeVerifier\":\"code-verifier\",\"nonce\":\"nonce\"}"));
    }

    private void assertError(final ResultActions actions, final int status, final String code) throws Exception {
        actions.andExpect(status().is(status)).andExpect(jsonPath("$.success").value(false)).andExpect(jsonPath("$.code").value(code))
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    private MvcResult authenticate(final String action, final String providerUserId) throws Exception {
        final MvcResult result = perform(action, providerUserId).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.accessToken").isString()).andReturn();
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Path=/api/v1/auth").contains("HttpOnly")
            .contains("SameSite=Lax").doesNotContain("Secure");
        return result;
    }

    private Long userIdOf(final String providerUserId) {
        return jdbc.queryForObject("SELECT user_id FROM user_providers WHERE provider_user_id = ? AND deleted_at IS NULL", Long.class,
            providerUserId);
    }

    private List<Integer> rowCounts() {
        return List.of("users", "user_providers", "carts", "point_balance", "refresh_sessions").stream()
            .map((final String table) -> jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).toList();
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
