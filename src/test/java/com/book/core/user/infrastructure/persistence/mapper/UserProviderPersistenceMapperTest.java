package com.book.core.user.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.user.domain.ProviderType;
import com.book.core.user.domain.UserProvider;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserProviderPersistenceMapperTest {
    @Test
    void 신규_회원_연결을_ID와_삭제_시각이_없는_Entity로_변환한다() {
        final var entity =
                UserProviderPersistenceMapper.toEntity(UserProvider.create(42L, ProviderType.KAKAO, "123", null));
        assertThat(entity.id()).isNull();
        assertThat(entity.userId()).isEqualTo(42L);
        assertThat(entity.providerType()).isEqualTo(ProviderType.KAKAO);
        assertThat(entity.providerUserId()).isEqualTo("123");
        assertThat(entity.providerEmail()).isNull();
        assertThat(entity.deletedAt()).isNull();
    }

    @Test
    void 저장된_회원_연결은_왕복_변환해도_삭제_상태가_유지된다() {
        final LocalDateTime deletedAt = LocalDateTime.of(2026, 9, 19, 12, 0);
        final var restored = UserProviderPersistenceMapper.toDomain(UserProviderPersistenceMapper.toEntity(
                UserProvider.restore(7L, 42L, ProviderType.KAKAO, "123", "book@example.com", deletedAt)));
        assertThat(restored.id()).isEqualTo(7L);
        assertThat(restored.userId()).isEqualTo(42L);
        assertThat(restored.providerEmail()).isEqualTo("book@example.com");
        assertThat(restored.deletedAt()).isEqualTo(deletedAt);
        assertThat(restored.isActive()).isFalse();
    }
}
