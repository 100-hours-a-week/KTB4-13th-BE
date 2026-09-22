package com.book.core.auth.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.BusinessException;
import com.book.common.exception.CommonErrorCode;
import com.book.core.auth.application.port.OAuthProviderClient;
import com.book.core.auth.application.port.RefreshSessionRepository;
import com.book.core.auth.application.port.RefreshTokenHasher;
import com.book.core.auth.application.usecase.RefreshSessionRegistrationUseCase;
import com.book.core.auth.domain.RefreshSession;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class RefreshSessionRepositoryIntegrationTest {
    private static final Instant EXPIRES_AT = Instant.parse("2030-01-09T03:04:05Z");

    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    RefreshSessionRepository refreshSessionRepository;

    @Autowired
    RefreshSessionRegistrationUseCase refreshSessionRegistrationUseCase;

    @Autowired
    RefreshTokenHasher refreshTokenHasher;

    @Autowired
    UserRepositoryPort userRepository;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    OAuthProviderClient oAuthProviderClient;

    @Test
    void session을_저장하고_active_user와_token_hash로_조회한다() {
        final User user = userRepository.save(User.create("세션조회회원"));
        final RefreshSession saved =
                refreshSessionRepository.save(RefreshSession.create(user.id(), "a".repeat(64), EXPIRES_AT));

        final RefreshSession foundByUser =
                refreshSessionRepository.findActiveByUserId(user.id()).orElseThrow();
        final RefreshSession foundByHash =
                refreshSessionRepository.findActiveByTokenHash("a".repeat(64)).orElseThrow();

        assertThat(foundByUser.id()).isEqualTo(saved.id());
        assertThat(foundByHash.id()).isEqualTo(saved.id());
        assertThat(foundByUser.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(jdbc.queryForObject(
                        "SELECT expires_at FROM refresh_sessions WHERE id = ?", LocalDateTime.class, saved.id()))
                .isEqualTo(LocalDateTime.ofInstant(EXPIRES_AT, ZoneOffset.UTC));
        assertThat(jdbc.queryForObject(
                        "SELECT created_at FROM refresh_sessions WHERE id = ?", LocalDateTime.class, saved.id()))
                .isNotNull();
        assertThat(jdbc.queryForObject(
                        "SELECT updated_at FROM refresh_sessions WHERE id = ?", LocalDateTime.class, saved.id()))
                .isNotNull();
        assertThat(jdbc.queryForObject(
                        "SELECT active_flag FROM refresh_sessions WHERE id = ?", Integer.class, saved.id()))
                .isEqualTo(1);
    }

    @Test
    void revoked_session은_active_조회에서_제외된다() {
        final User user = userRepository.save(User.create("세션해제회원"));
        final RefreshSession session =
                refreshSessionRepository.save(RefreshSession.create(user.id(), "b".repeat(64), EXPIRES_AT));
        session.revoke(Instant.parse("2030-01-03T03:04:05Z"));

        refreshSessionRepository.save(session);

        assertThat(refreshSessionRepository.findActiveByUserId(user.id())).isEmpty();
        assertThat(refreshSessionRepository.findActiveByTokenHash("b".repeat(64)))
                .isEmpty();
        assertThat(jdbc.queryForObject(
                        "SELECT active_flag FROM refresh_sessions WHERE id = ?", Integer.class, session.id()))
                .isNull();
    }

    @Test
    void 같은_user의_active_session은_두_개를_저장할_수_없다() {
        final User user = userRepository.save(User.create("세션중복회원"));
        refreshSessionRepository.save(RefreshSession.create(user.id(), "c".repeat(64), EXPIRES_AT));

        assertThatThrownBy(() ->
                        refreshSessionRepository.save(RefreshSession.create(user.id(), "d".repeat(64), EXPIRES_AT)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(CommonErrorCode.STORAGE_FAILURE));
    }

    @Test
    void revoke한_후에는_같은_user의_새_session을_저장할_수_있다() {
        final User user = userRepository.save(User.create("세션교체회원"));
        final RefreshSession oldSession =
                refreshSessionRepository.save(RefreshSession.create(user.id(), "e".repeat(64), EXPIRES_AT));
        oldSession.revoke(Instant.parse("2030-01-03T03:04:05Z"));
        refreshSessionRepository.save(oldSession);

        final RefreshSession newSession = refreshSessionRepository.save(
                RefreshSession.create(user.id(), "f".repeat(64), EXPIRES_AT.plusSeconds(60)));

        assertThat(refreshSessionRepository
                        .findActiveByUserId(user.id())
                        .orElseThrow()
                        .id())
                .isEqualTo(newSession.id());
    }

    @Test
    void 존재하지_않는_user_ID의_session은_FK가_차단한다() {
        assertThatThrownBy(() -> refreshSessionRepository.save(
                        RefreshSession.create(Long.MAX_VALUE, "1".repeat(64), EXPIRES_AT)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(CommonErrorCode.STORAGE_FAILURE));
    }

    @Test
    void session_등록은_원문이_아닌_hash를_저장하고_기존_session을_revoke한다() {
        final User user = userRepository.save(User.create("로그인세션회원"));
        refreshSessionRegistrationUseCase.execute(user.id(), "old-refresh-token", EXPIRES_AT);

        refreshSessionRegistrationUseCase.execute(user.id(), "new-refresh-token", EXPIRES_AT.plusSeconds(60));

        final String newTokenHash = refreshTokenHasher.hash("new-refresh-token");
        final RefreshSession active =
                refreshSessionRepository.findActiveByUserId(user.id()).orElseThrow();
        assertThat(active.tokenHash()).isEqualTo(newTokenHash).isNotEqualTo("new-refresh-token");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM refresh_sessions WHERE user_id = ? AND revoked_at IS NOT NULL",
                        Integer.class,
                        user.id()))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM refresh_sessions WHERE user_id = ? AND active_flag = 1",
                        Integer.class,
                        user.id()))
                .isEqualTo(1);
    }

    @Test
    void 새_session_저장이_실패하면_기존_session_revoke도_rollback된다() {
        final User user = userRepository.save(User.create("세션롤백회원"));
        refreshSessionRegistrationUseCase.execute(user.id(), "preserved-refresh-token", EXPIRES_AT);
        final String oldTokenHash = refreshTokenHasher.hash("preserved-refresh-token");

        assertThatThrownBy(() -> refreshSessionRegistrationUseCase.execute(user.id(), "failed-refresh-token", null))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(CommonErrorCode.STORAGE_FAILURE));

        final RefreshSession active =
                refreshSessionRepository.findActiveByUserId(user.id()).orElseThrow();
        assertThat(active.tokenHash()).isEqualTo(oldTokenHash);
        assertThat(active.revokedAt()).isNull();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM refresh_sessions WHERE user_id = ?", Integer.class, user.id()))
                .isEqualTo(1);
    }
}
