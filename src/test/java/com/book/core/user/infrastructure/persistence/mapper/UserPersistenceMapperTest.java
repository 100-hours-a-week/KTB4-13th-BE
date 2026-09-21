package com.book.core.user.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.user.domain.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserPersistenceMapperTest {
    @Test
    void 신규_회원을_ID와_삭제_시각이_없는_Entity로_변환한다() {
        final var entity = UserPersistenceMapper.toEntity(User.create("북적이"));
        assertThat(entity.id()).isNull();
        assertThat(entity.nickname()).isEqualTo("북적이");
        assertThat(entity.deletedAt()).isNull();
    }

    @Test
    void 저장된_회원은_왕복_변환해도_삭제_상태가_유지된다() {
        final LocalDateTime deletedAt = LocalDateTime.of(2026, 9, 19, 12, 0);
        final var restored =
                UserPersistenceMapper.toDomain(UserPersistenceMapper.toEntity(User.restore(42L, "북적이", deletedAt)));
        assertThat(restored.id()).isEqualTo(42L);
        assertThat(restored.nickname()).isEqualTo("북적이");
        assertThat(restored.deletedAt()).isEqualTo(deletedAt);
        assertThat(restored.isActive()).isFalse();
    }
}
