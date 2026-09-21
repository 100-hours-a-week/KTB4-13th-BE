package com.book.core.user.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.BusinessException;
import com.book.common.exception.CommonErrorCode;
import com.book.core.user.application.command.UserResolveCommand;
import com.book.core.user.application.port.UserProviderRepository;
import com.book.core.user.application.port.UserRepository;
import com.book.core.user.application.usecase.UserRegistrationUseCase;
import com.book.core.user.domain.ProviderType;
import com.book.core.user.domain.User;
import com.book.core.user.domain.UserProvider;
import com.book.core.user.domain.exception.UserErrorCode;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class UserRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserProviderRepository userProviderRepository;

    @Autowired
    UserRegistrationUseCase userRegistrationUseCase;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void DB가_timestamp와_activeFlag를_생성하고_nullable_email을_저장한다() {
        final User user = userRepository.save(User.create("DB생성회원"));
        final UserProvider userProvider = userProviderRepository.save(
                UserProvider.create(user.id(), ProviderType.KAKAO, "timestamp-provider", null));

        final User foundUser = userRepository.findById(user.id()).orElseThrow();
        final UserProvider foundUserProvider = userProviderRepository
                .findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "timestamp-provider")
                .orElseThrow();
        assertThat(foundUser.nickname()).isEqualTo(user.nickname());
        assertThat(foundUserProvider.id()).isEqualTo(userProvider.id());
        assertThat(userProvider.providerEmail()).isNull();
        assertThat(jdbc.queryForObject("SELECT created_at FROM users WHERE id = ?", Timestamp.class, user.id()))
                .isNotNull();
        assertThat(jdbc.queryForObject("SELECT updated_at FROM users WHERE id = ?", Timestamp.class, user.id()))
                .isNotNull();
        assertThat(jdbc.queryForObject("SELECT active_flag FROM users WHERE id = ?", Integer.class, user.id()))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT active_flag FROM user_providers WHERE id = ?", Integer.class, userProvider.id()))
                .isEqualTo(1);
    }

    @Test
    void 활성_회원의_닉네임은_중복될_수_없고_soft_delete_후에는_재사용할_수_있다() {
        final User first = userRepository.save(User.create("재사용닉네임"));

        assertThatThrownBy(() -> userRepository.save(User.create("재사용닉네임")))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(UserErrorCode.NICKNAME_CONFLICT));

        jdbc.update("UPDATE users SET deleted_at = CURRENT_TIMESTAMP(6) WHERE id = ?", first.id());
        assertThat(jdbc.queryForObject("SELECT active_flag FROM users WHERE id = ?", Integer.class, first.id()))
                .isNull();

        final User reused = userRepository.save(User.create("재사용닉네임"));
        assertThat(reused.id()).isNotEqualTo(first.id());
    }

    @Test
    void 활성_provider_identity는_중복될_수_없고_soft_delete_후에는_재사용할_수_있다() {
        final User user = userRepository.save(User.create("연결재사용회원"));
        final UserProvider first = userProviderRepository.save(
                UserProvider.create(user.id(), ProviderType.KAKAO, "reusable-provider", "book@example.com"));

        assertThatThrownBy(() -> userProviderRepository.save(
                        UserProvider.create(user.id(), ProviderType.KAKAO, "reusable-provider", null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(UserErrorCode.PROVIDER_IDENTITY_CONFLICT));

        jdbc.update("UPDATE user_providers SET deleted_at = CURRENT_TIMESTAMP(6) WHERE id = ?", first.id());
        assertThat(userProviderRepository.findActiveByProviderTypeAndProviderUserId(
                        ProviderType.KAKAO, "reusable-provider"))
                .isEmpty();
        assertThat(jdbc.queryForObject(
                        "SELECT active_flag FROM user_providers WHERE id = ?", Integer.class, first.id()))
                .isNull();

        final UserProvider reused = userProviderRepository.save(
                UserProvider.create(user.id(), ProviderType.KAKAO, "reusable-provider", null));
        assertThat(reused.id()).isNotEqualTo(first.id());
        assertThat(userProviderRepository
                        .findActiveByProviderTypeAndProviderUserId(ProviderType.KAKAO, "reusable-provider")
                        .orElseThrow()
                        .id())
                .isEqualTo(reused.id());
    }

    @Test
    void 존재하지_않는_회원_ID의_provider_연결은_FK가_차단한다() {
        assertThatThrownBy(() -> userProviderRepository.save(
                        UserProvider.create(Long.MAX_VALUE, ProviderType.KAKAO, "orphan-provider", null)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(CommonErrorCode.STORAGE_FAILURE));
    }

    @Test
    void nickname_충돌_시도는_provider를_저장하지_않는다() {
        userRepository.save(User.create("가입충돌닉네임"));
        final UserResolveCommand command =
                new UserResolveCommand(ProviderType.KAKAO, "nickname-rollback-provider", null);

        assertThatThrownBy(() -> userRegistrationUseCase.execute(command, "가입충돌닉네임"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(UserErrorCode.NICKNAME_CONFLICT));
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM user_providers WHERE provider_user_id = ?",
                        Integer.class,
                        "nickname-rollback-provider"))
                .isZero();
    }

    @Test
    void provider_identity_충돌_시도는_먼저_저장한_User까지_rollback한다() {
        final User existingUser = userRepository.save(User.create("기존연결회원"));
        userProviderRepository.save(
                UserProvider.create(existingUser.id(), ProviderType.KAKAO, "registration-conflict", null));
        final UserResolveCommand command = new UserResolveCommand(ProviderType.KAKAO, "registration-conflict", null);

        assertThatThrownBy(() -> userRegistrationUseCase.execute(command, "롤백대상회원"))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(UserErrorCode.PROVIDER_IDENTITY_CONFLICT));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE nickname = ?", Integer.class, "롤백대상회원"))
                .isZero();
    }
}
