package com.book.core.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.BusinessException;
import com.book.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class UserTest {
    @Test
    void 신규_회원의_닉네임을_정규화하여_활성_상태로_생성한다() {
        final User user = User.create("  북적이  ");
        assertThat(user.id()).isNull();
        assertThat(user.nickname()).isEqualTo("북적이");
        assertThat(user.deletedAt()).isNull();
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void 공백_제거_후_2자에서_20자까지_허용한다() {
        assertThat(User.create("  가나  ").nickname()).isEqualTo("가나");
        assertThat(User.create("가".repeat(20)).nickname()).hasSize(20);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "한", "\t\n", " "})
    void 비어_있거나_2자_미만인_닉네임을_거부한다(final String nickname) {
        assertThatThrownBy(() -> User.create(nickname)).isInstanceOfSatisfying(BusinessException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_NICKNAME));
    }

    @Test
    void 닉네임이_21자이면_거부한다() {
        assertThatThrownBy(() -> User.create("가".repeat(21))).isInstanceOfSatisfying(BusinessException.class,
            (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_NICKNAME));
    }

    @Test
    void 저장_회원은_활성_상태로_복원한다() {
        final User user = User.restore(42L, "북적이");
        assertThat(user.id()).isEqualTo(42L);
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void 복원한_회원을_삭제하면_비활성_상태가_된다() {
        final User user = User.restore(42L, "북적이");
        user.delete();
        assertThat(user.isActive()).isFalse();
        assertThat(user.isDeleted()).isTrue();
    }

    @Test
    void 복원할_회원_ID가_양수가_아니면_거부한다() {
        assertThatThrownBy(() -> User.restore(null, "북적이")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> User.restore(0L, "북적이")).isInstanceOf(BusinessException.class);
    }
}
