package com.book.core.auth.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.book.core.auth.infrastructure.persistence.entity.RefreshSessionEntity;
import com.book.core.user.application.port.UserRepositoryPort;
import com.book.core.user.domain.User;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class RefreshSessionRepositoryIntegrationTest {
    private static final Instant EXPIRES_AT = Instant.parse("2030-01-09T03:04:05Z");
    @Autowired
    RefreshSessionJpaRepository jpaRepository;

    @Autowired
    UserRepositoryPort userRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void session을_저장하고_active_user와_token_hash로_조회한다() {
        final User user = userRepository.save(User.create("세션조회회원"));
        final RefreshSessionEntity saved =
            jpaRepository.saveAndFlush(new RefreshSessionEntity(null, user.id(), "a".repeat(64), EXPIRES_AT, null));

        final RefreshSessionEntity foundByUser = jpaRepository.findByUserIdAndRevokedAtIsNull(user.id()).orElseThrow();
        final RefreshSessionEntity foundByHash = jpaRepository.findByTokenHashAndRevokedAtIsNull("a".repeat(64)).orElseThrow();

        assertThat(foundByUser.id()).isEqualTo(saved.id());
        assertThat(foundByHash.id()).isEqualTo(saved.id());
        assertThat(foundByUser.expiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(jdbc.queryForObject("SELECT expires_at FROM refresh_sessions WHERE id = ?", LocalDateTime.class, saved.id()))
            .isEqualTo(LocalDateTime.ofInstant(EXPIRES_AT, ZoneOffset.UTC));
        assertThat(jdbc.queryForObject("SELECT created_at FROM refresh_sessions WHERE id = ?", LocalDateTime.class, saved.id()))
            .isNotNull();
        assertThat(jdbc.queryForObject("SELECT updated_at FROM refresh_sessions WHERE id = ?", LocalDateTime.class, saved.id()))
            .isNotNull();
        assertThat(jdbc.queryForObject("SELECT active_flag FROM refresh_sessions WHERE id = ?", Integer.class, saved.id())).isEqualTo(1);
    }

    @Test
    void revoked_session은_active_조회에서_제외된다() {
        final User user = userRepository.save(User.create("세션해제회원"));
        final RefreshSessionEntity session =
            jpaRepository.saveAndFlush(new RefreshSessionEntity(null, user.id(), "b".repeat(64), EXPIRES_AT, null));
        jpaRepository.saveAndFlush(
            new RefreshSessionEntity(session.id(), user.id(), "b".repeat(64), EXPIRES_AT, Instant.parse("2030-01-03T03:04:05Z")));

        assertThat(jpaRepository.findByUserIdAndRevokedAtIsNull(user.id())).isEmpty();
        assertThat(jpaRepository.findByTokenHashAndRevokedAtIsNull("b".repeat(64))).isEmpty();
        assertThat(jdbc.queryForObject("SELECT active_flag FROM refresh_sessions WHERE id = ?", Integer.class, session.id())).isNull();
    }

    @Test
    void 같은_user의_active_session은_두_개를_저장하면_기술_예외가_그대로_전파된다() {
        final User user = userRepository.save(User.create("세션중복회원"));
        jpaRepository.saveAndFlush(new RefreshSessionEntity(null, user.id(), "c".repeat(64), EXPIRES_AT, null));

        assertThatThrownBy(() -> jpaRepository.saveAndFlush(new RefreshSessionEntity(null, user.id(), "d".repeat(64), EXPIRES_AT, null)))
            .isInstanceOf(DataAccessException.class);
    }

    @Test
    void revoke한_후에는_같은_user의_새_session을_저장할_수_있다() {
        final User user = userRepository.save(User.create("세션교체회원"));
        final RefreshSessionEntity oldSession =
            jpaRepository.saveAndFlush(new RefreshSessionEntity(null, user.id(), "e".repeat(64), EXPIRES_AT, null));
        jpaRepository.saveAndFlush(
            new RefreshSessionEntity(oldSession.id(), user.id(), "e".repeat(64), EXPIRES_AT, Instant.parse("2030-01-03T03:04:05Z")));

        final RefreshSessionEntity newSession =
            jpaRepository.saveAndFlush(new RefreshSessionEntity(null, user.id(), "f".repeat(64), EXPIRES_AT.plusSeconds(60), null));

        assertThat(jpaRepository.findByUserIdAndRevokedAtIsNull(user.id()).orElseThrow().id()).isEqualTo(newSession.id());
    }

    @Test
    void 존재하지_않는_user_ID의_session은_FK_기술_예외를_그대로_전파한다() {
        assertThatThrownBy(
            () -> jpaRepository.saveAndFlush(new RefreshSessionEntity(null, Long.MAX_VALUE, "1".repeat(64), EXPIRES_AT, null)))
            .isInstanceOf(DataAccessException.class);
    }

}
