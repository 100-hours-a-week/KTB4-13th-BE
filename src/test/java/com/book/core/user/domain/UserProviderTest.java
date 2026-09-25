package com.book.core.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserProviderTest {
    @Test
    void 신규_회원과_카카오_계정의_연결을_생성한다() {
        final UserProvider userProvider = UserProvider.create(42L, ProviderType.KAKAO, "123456789", null);
        assertThat(userProvider.id()).isNull();
        assertThat(userProvider.userId()).isEqualTo(42L);
        assertThat(userProvider.providerType()).isEqualTo(ProviderType.KAKAO);
        assertThat(userProvider.providerUserId()).isEqualTo("123456789");
        assertThat(userProvider.providerEmail()).isNull();
        assertThat(userProvider.deletedAt()).isNull();
        assertThat(userProvider.isActive()).isTrue();
    }

    @Test
    void 삭제_시각이_없는_저장_회원_연결은_활성_상태로_복원한다() {
        final UserProvider userProvider =
                UserProvider.restore(7L, 42L, ProviderType.KAKAO, "123456789", "book@example.com", null);
        assertThat(userProvider.id()).isEqualTo(7L);
        assertThat(userProvider.providerEmail()).isEqualTo("book@example.com");
        assertThat(userProvider.deletedAt()).isNull();
        assertThat(userProvider.isActive()).isTrue();
    }

    @Test
    void 삭제_시각이_있는_저장_회원_연결은_비활성_상태로_복원한다() {
        final LocalDateTime deletedAt = LocalDateTime.of(2026, 9, 19, 12, 0);
        final UserProvider userProvider =
                UserProvider.restore(7L, 42L, ProviderType.KAKAO, "123456789", null, deletedAt);
        assertThat(userProvider.providerEmail()).isNull();
        assertThat(userProvider.deletedAt()).isEqualTo(deletedAt);
        assertThat(userProvider.isActive()).isFalse();
    }

    @Test
    void providerUserId는_255자와_providerEmail은_254자까지_허용한다() {
        final UserProvider userProvider =
                UserProvider.create(42L, ProviderType.KAKAO, "a".repeat(255), "a".repeat(254));
        assertThat(userProvider.providerUserId()).hasSize(255);
        assertThat(userProvider.providerEmail()).hasSize(254);
    }

    @Test
    void 유효하지_않은_회원_ID와_providerType을_거부한다() {
        assertThatThrownBy(() -> UserProvider.create(null, ProviderType.KAKAO, "123", null))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_USER_ID));
        assertThatThrownBy(() -> UserProvider.create(42L, null, "123", null))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_PROVIDER_TYPE));
    }

    @Test
    void 비어_있거나_255자를_초과한_providerUserId를_거부한다() {
        assertThatThrownBy(() -> UserProvider.create(42L, ProviderType.KAKAO, " ", null))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_PROVIDER_USER_ID));
        assertThatThrownBy(() -> UserProvider.create(42L, ProviderType.KAKAO, "a".repeat(256), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void providerEmail이_254자를_초과하면_거부한다() {
        assertThatThrownBy(() -> UserProvider.create(42L, ProviderType.KAKAO, "123", "a".repeat(255)))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_PROVIDER_EMAIL));
    }

    @Test
    void 복원할_회원_연결_ID가_양수가_아니면_거부한다() {
        assertThatThrownBy(() -> UserProvider.restore(null, 42L, ProviderType.KAKAO, "123", null, null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> UserProvider.restore(0L, 42L, ProviderType.KAKAO, "123", null, null))
                .isInstanceOfSatisfying(
                        BusinessException.class,
                        (final var exception) ->
                                assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_USER_PROVIDER_ID));
    }
}
