package com.book.core.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import com.book.core.auth.application.port.OAuthProviderClient;
import com.book.core.auth.application.port.OAuthTokenClient;
import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.auth.application.result.OAuthUserInfo;
import com.book.core.auth.application.usecase.CreateLoginSessionUseCase;
import com.book.core.auth.domain.RefreshSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.sql.DriverManager;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test", "mysql-test"})
@Testcontainers
@Tag("integration")
@Tag("mysql-migration")
class AuthSessionHttpMySqlTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");
    @LocalServerPort
    int port;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    CreateLoginSessionUseCase login;
    @Autowired
    @Qualifier("refreshJwtDecoder")
    JwtDecoder refreshDecoder;
    @Autowired
    @Qualifier("serviceJwtDecoder")
    JwtDecoder accessDecoder;
    @MockitoBean
    OAuthTokenClient oauthToken;
    @MockitoBean
    OAuthProviderClient oauthProvider;
    @MockitoSpyBean
    RefreshSessionRepository sessions;
    @MockitoSpyBean
    RefreshTokenHasher hasher;
    final HttpClient client = HttpClient.newHttpClient();

    @Test
    void overlappingLoginsBothReturnValidHttpAndOnlyLatestRtSurvives() throws Exception {
        givenIdentity("http-race");
        final var initial = request("signup", "http-race");
        assertThat(initial.statusCode()).isEqualTo(200);
        final long userId = userId("http-race");
        assertThat(AopUtils.isAopProxy(login)).isTrue();
        assertThat(jdbc.queryForObject("SELECT @@transaction_isolation", String.class)).isEqualTo("REPEATABLE-READ");
        final var inserted = new CountDownLatch(1);
        final var release = new CountDownLatch(1);
        final var held = new AtomicBoolean();
        doAnswer((final var invocation) -> {
            final RefreshSession session = invocation.getArgument(0);
            final Object saved = invocation.callRealMethod();
            if (session.userId().equals(userId) && session.id() == null && held.compareAndSet(false, true)) {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                inserted.countDown();
                if (!release.await(15, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("timed out");
                }
            }
            return saved;
        }).when(sessions).save(any(RefreshSession.class));
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var first = executor.submit(() -> request("login", "http-race"));
            assertThat(inserted.await(15, TimeUnit.SECONDS)).isTrue();
            final var second = executor.submit(() -> request("login", "http-race"));
            try {
                final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                int waits = 0;
                while (System.nanoTime() < deadline && waits == 0) {
                    waits = lockWaitCount();
                    if (waits == 0) {
                        Thread.sleep(25);
                    }
                }
                assertThat(waits).as("MySQL records the second transaction waiting for the user lock").isPositive();
            } finally {
                release.countDown();
            }
            final var a = first.get(15, TimeUnit.SECONDS);
            final var b = second.get(15, TimeUnit.SECONDS);
            assertResponse(a, userId);
            assertResponse(b, userId);
            final String oldToken = cookie(a);
            final String currentToken = cookie(b);
            assertThat(currentToken).isNotEqualTo(oldToken);
            assertThat(activeCount(userId)).isEqualTo(1);
            assertThat(activeHash(userId)).isEqualTo(hasher.hash(currentToken)).isNotEqualTo(hasher.hash(oldToken));
            assertThat(reissue(oldToken).statusCode()).isEqualTo(401);
            assertThat(reissue(currentToken).statusCode()).isEqualTo(200);
        }
    }

    @Test
    void failedInsertRollsBackRevokeAndReturnsNoCookie() throws Exception {
        givenIdentity("http-rollback");
        final var signup = request("signup", "http-rollback");
        assertThat(signup.statusCode()).isEqualTo(200);
        final long userId = userId("http-rollback");
        final String oldToken = cookie(signup);
        final String oldHash = activeHash(userId);
        final var revokedFlushed = new AtomicBoolean();
        final var insertFailure = new AtomicReference<Throwable>();
        doAnswer((final var invocation) -> {
            final RefreshSession session = invocation.getArgument(0);
            try {
                final Object result = invocation.callRealMethod();
                if (session.userId().equals(userId) && session.id() != null && !session.isActive()) {
                    revokedFlushed.set(true);
                }
                return result;
            } catch (final Throwable failure) {
                if (session.id() == null) {
                    insertFailure.set(failure);
                }
                throw failure;
            }
        }).when(sessions).save(any(RefreshSession.class));
        doAnswer((final var invocation) -> "x".repeat(65)).when(hasher).hash(any(String.class));
        final var failed = request("login", "http-rollback");
        assertThat(revokedFlushed).isTrue();
        assertThat(insertFailure.get()).isNotNull();
        Throwable cause = insertFailure.get();
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        assertThat(cause).isInstanceOfSatisfying(java.sql.SQLException.class,
            (final var exception) -> assertThat(exception.getErrorCode()).isEqualTo(1406));
        assertThat(failed.statusCode()).isEqualTo(500);
        assertThat(failed.headers().allValues("set-cookie")).isEmpty();
        assertThat(failed.body()).contains("\"success\":false");
        assertThat(activeCount(userId)).isEqualTo(1);
        assertThat(activeHash(userId)).isEqualTo(oldHash);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM refresh_sessions WHERE user_id=?", Integer.class, userId)).isEqualTo(1);
        org.mockito.Mockito.reset(hasher);
        assertThat(reissue(oldToken).statusCode()).isEqualTo(200);
        assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() "
                + "AND table_name='refresh_sessions' AND index_name='uk_refresh_sessions_user_active_flag' AND non_unique=0",
            Integer.class)).isEqualTo(2);
    }

    private int lockWaitCount() throws Exception {
        try (final var connection = DriverManager.getConnection(mysql.getJdbcUrl(), "root", mysql.getPassword());
            final var statement = connection.createStatement();
            final var result =
                statement.executeQuery("SELECT COUNT(*) FROM performance_schema.data_lock_waits w JOIN performance_schema.data_locks l "
                    + "ON w.REQUESTING_ENGINE_LOCK_ID=l.ENGINE_LOCK_ID AND w.ENGINE=l.ENGINE WHERE l.OBJECT_NAME='users' "
                    + "AND l.INDEX_NAME='PRIMARY'")) {
            result.next();
            return result.getInt(1);
        }
    }

    private void givenIdentity(final String id) {
        when(oauthToken.exchangeForIdToken(id + "-code", "verifier")).thenReturn(id + "-id");
        when(oauthProvider.verify(id + "-id", "nonce")).thenReturn(new OAuthUserInfo(id, null));
    }

    private HttpResponse<String> request(final String action, final String id) throws Exception {
        return send("/api/v1/auth/kakao/" + action,
            "{\"authorizationCode\":\"" + id + "-code\",\"codeVerifier\":\"verifier\",\"nonce\":\"nonce\"}", null);
    }

    private HttpResponse<String> reissue(final String token) throws Exception {
        return send("/api/v1/auth/reissue", "", "refreshToken=" + token);
    }

    private HttpResponse<String> send(final String path, final String body, final String cookie) throws Exception {
        final var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void assertResponse(final HttpResponse<String> response, final long userId) {
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"success\":true");
        final var matcher = Pattern.compile("\\\"accessToken\\\":\\\"([^\\\"]+)\\\"").matcher(response.body());
        assertThat(matcher.find()).isTrue();
        assertThat(accessDecoder.decode(matcher.group(1)).getSubject()).isEqualTo(Long.toString(userId));
        final String setCookie = response.headers().firstValue("set-cookie").orElseThrow();
        assertThat(setCookie).contains("Path=/api/v1/auth", "HttpOnly", "SameSite=Lax");
        assertThat(refreshDecoder.decode(cookie(response)).getSubject()).isEqualTo(Long.toString(userId));
    }

    private String cookie(final HttpResponse<String> response) {
        return response.headers().firstValue("set-cookie").orElseThrow().split(";", 2)[0].substring("refreshToken=".length());
    }

    private long userId(final String providerId) {
        return jdbc.queryForObject("SELECT user_id FROM user_providers WHERE provider_user_id=?", Long.class, providerId);
    }

    private int activeCount(final long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM refresh_sessions WHERE user_id=? AND revoked_at IS NULL", Integer.class, userId);
    }

    private String activeHash(final long userId) {
        return jdbc.queryForObject("SELECT token_hash FROM refresh_sessions WHERE user_id=? AND revoked_at IS NULL", String.class, userId);
    }
}
