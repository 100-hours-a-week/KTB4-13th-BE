package com.book.core.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import com.book.core.auth.application.result.OAuthUserInfo;
import com.book.core.auth.application.port.OAuthProviderClient;
import com.book.core.auth.application.port.OAuthTokenClient;
import com.book.core.user.application.port.OAuthUnlinkClient;
import com.book.core.user.application.port.UserProviderRepositoryPort;
import com.book.core.user.application.service.UserService;
import com.book.core.user.application.usecase.UnlinkWithdrawalProvidersUseCase;
import com.book.core.onboarding.application.usecase.DeleteUserOnboardingDataUseCase;
import com.book.core.user.domain.ProviderType;
import com.book.core.user.domain.UserProvider;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class UserWithdrawalIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    UserService userService;

    @Autowired
    TransactionTemplate transactionTemplate;

    @Autowired
    UserProviderRepositoryPort userProviderRepository;

    @Autowired
    UnlinkWithdrawalProvidersUseCase unlinkWithdrawalProvidersUseCase;

    @MockitoBean
    OAuthProviderClient oAuthProviderClient;

    @MockitoBean
    OAuthTokenClient oAuthTokenClient;

    @MockitoBean
    OAuthUnlinkClient oAuthUnlinkClient;

    @MockitoSpyBean
    DeleteUserOnboardingDataUseCase deleteUserOnboardingDataUseCase;

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
    void 탈퇴_신청_7일이_지나면_복구와_재가입을_막고_연결_해제_확인_후_유예_기간이_지나야_파기하며_늦게_끝난_이전_실행은_새_계정을_건드리지_않는다() throws Exception {
        final String accessToken = accessToken(signup("expired-provider").andExpect(status().isOk()).andReturn());
        final Long userId = userIdOf("expired-provider");
        jdbc.update("INSERT INTO user_onboardings (user_id, onboarding_status) VALUES (?, 'IN_PROGRESS')", userId);
        jdbc.update("INSERT INTO user_onboarding_answers (user_id, onboarding_option_id) SELECT ?, MIN(id) FROM onboarding_options",
            userId);
        jdbc.update("INSERT INTO books (id, title, author, publisher, category, published_at) VALUES (?, ?, ?, ?, ?, ?)", 9001L, "도서", "저자",
            "출판사", "소설", "2026-01-01");
        jdbc.update("INSERT INTO user_onboarding_books (user_id, book_id) VALUES (?, ?)", userId, 9001L);
        jdbc.update("INSERT INTO books (id, title, author, publisher, category, published_at) VALUES (?, ?, ?, ?, ?, ?)", 9002L, "도서", "저자",
            "출판사", "소설", "2026-01-01");
        jdbc.update("INSERT INTO user_library_books (user_id, book_id, added_at) VALUES (?, ?, CURRENT_TIMESTAMP(6))", userId, 9001L);
        jdbc.update("INSERT INTO user_library_books (user_id, book_id, added_at, deleted_at) VALUES (?, ?, CURRENT_TIMESTAMP(6), ?)",
            userId, 9002L, "2026-01-01 00:00:00");
        mvc.perform(delete("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)).andExpect(status().isOk());
        jdbc.update("UPDATE users SET delete_requested_at = ? WHERE id = ?", LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(8),
            userId);
        final UserProvider staleProvider = userProviderRepository.findActiveByUserId(userId).getFirst();

        login("expired-provider").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("USER_WITHDRAWAL_PENDING"));

        doThrow(new BusinessException(ErrorCode.OAUTH_PROVIDER_UNAVAILABLE)).when(oAuthUnlinkClient).unlink(ProviderType.KAKAO,
            "expired-provider");
        userService.completeUserDeletion();

        assertThat(count("SELECT COUNT(*) FROM users WHERE id = ? AND deleted_at IS NULL", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM user_providers WHERE user_id = ? AND unlinked_at IS NULL", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM user_onboarding_answers WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM user_library_books WHERE user_id = ? AND deleted_at IS NULL", userId)).isEqualTo(1);

        doNothing().when(oAuthUnlinkClient).unlink(ProviderType.KAKAO, "expired-provider");
        doThrow(new IllegalStateException("db cleanup failure")).when(deleteUserOnboardingDataUseCase).execute(userId);
        userService.completeUserDeletion();

        assertThat(count("SELECT COUNT(*) FROM users WHERE id = ? AND deleted_at IS NULL", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM user_providers WHERE user_id = ? AND unlinked_at IS NOT NULL AND deleted_at IS NULL "
            + "AND provider_user_id = 'expired-provider'", userId)).isEqualTo(1);

        doCallRealMethod().when(deleteUserOnboardingDataUseCase).execute(userId);
        userService.completeUserDeletion();

        assertThat(count("SELECT COUNT(*) FROM users WHERE id = ? AND deleted_at IS NULL", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM user_onboarding_answers WHERE user_id = ?", userId)).isZero();
        login("expired-provider").andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("USER_WITHDRAWAL_PENDING"));

        jdbc.update("UPDATE user_providers SET unlinked_at = ? WHERE user_id = ?",
            LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusHours(13), userId);
        userService.completeUserDeletion();

        verify(oAuthUnlinkClient, times(2)).unlink(ProviderType.KAKAO, "expired-provider");
        assertThat(count("SELECT COUNT(*) FROM users WHERE id = ? AND deleted_at IS NOT NULL AND nickname = '탈퇴한 회원'", userId))
            .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM user_providers WHERE user_id = ? AND unlinked_at IS NOT NULL AND deleted_at IS NOT NULL "
            + "AND provider_email IS NULL AND provider_user_id <> 'expired-provider'", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM user_onboardings WHERE user_id = ?", userId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM user_onboarding_answers WHERE user_id = ?", userId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM user_onboarding_books WHERE user_id = ?", userId)).isZero();
        assertThat(count("SELECT COUNT(*) FROM user_library_books WHERE user_id = ? AND deleted_at IS NOT NULL", userId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT deleted_at FROM user_library_books WHERE user_id = ? AND book_id = ?", LocalDateTime.class,
            userId, 9002L)).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        assertThat(count("SELECT COUNT(*) FROM refresh_sessions WHERE user_id = ?", userId)).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT TABLE_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA = DATABASE() "
            + "AND REFERENCED_TABLE_NAME = 'users'", String.class)).as("users를 참조하는 테이블이 추가되면 최종 탈퇴 보존·파기 대상을 결정해야 합니다.")
            .containsExactlyInAnyOrder("user_providers", "refresh_sessions", "user_onboardings", "user_onboarding_answers",
                "user_onboarding_books", "recommendation_cards", "user_term_agreement", "user_library_books");

        signup("expired-provider").andExpect(status().isOk());
        final Long newUserId = userIdOf("expired-provider");
        assertThat(newUserId).isNotEqualTo(userId);

        unlinkWithdrawalProvidersUseCase.execute(List.of(staleProvider));

        assertThat(count("SELECT COUNT(*) FROM user_providers WHERE user_id = ? AND deleted_at IS NOT NULL "
            + "AND provider_user_id <> 'expired-provider'", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM user_providers WHERE user_id = ? AND deleted_at IS NULL AND unlinked_at IS NULL", newUserId))
            .isEqualTo(1);
        login("expired-provider").andExpect(status().isOk());
        assertThat(userIdOf("expired-provider")).isEqualTo(newUserId);
    }

    @Test
    void 최종_탈퇴는_진행_중인_복구_트랜잭션을_PK_잠금으로_기다린_뒤_복구된_회원의_연결_해제와_파기를_하지_않는다() throws Exception {
        final String accessToken = accessToken(signup("race-provider").andExpect(status().isOk()).andReturn());
        final Long userId = userIdOf("race-provider");
        jdbc.update("INSERT INTO user_onboardings (user_id, onboarding_status) VALUES (?, 'IN_PROGRESS')", userId);
        mvc.perform(delete("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)).andExpect(status().isOk());
        jdbc.update("UPDATE users SET delete_requested_at = ? WHERE id = ?", LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(8),
            userId);
        final CountDownLatch recoveryLocked = new CountDownLatch(1);
        final CountDownLatch releaseRecovery = new CountDownLatch(1);

        final CompletableFuture<Void> recovery =
            CompletableFuture.runAsync(() -> transactionTemplate.executeWithoutResult((final var tx) -> {
                jdbc.update("UPDATE users SET delete_requested_at = NULL WHERE id = ?", userId);
                recoveryLocked.countDown();
                await(releaseRecovery);
            }));
        await(recoveryLocked);
        final CompletableFuture<Void> withdrawal = CompletableFuture.runAsync(userService::completeUserDeletion);
        Thread.sleep(500);

        assertThat(withdrawal).isNotDone();
        releaseRecovery.countDown();
        recovery.get(10, TimeUnit.SECONDS);
        withdrawal.get(10, TimeUnit.SECONDS);
        verify(oAuthUnlinkClient, never()).unlink(ProviderType.KAKAO, "race-provider");
        assertThat(count("SELECT COUNT(*) FROM users WHERE id = ? AND deleted_at IS NULL", userId)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM user_onboardings WHERE user_id = ?", userId)).isEqualTo(1);
    }

    private static void await(final CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("latch timeout");
            }
        } catch (final InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
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
