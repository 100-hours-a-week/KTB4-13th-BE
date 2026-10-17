package com.book.core.memory.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class UserMemoryTest {
    @ParameterizedTest
    @ValueSource(ints = {-4, 0, 4})
    void 세기가_범위_밖이거나_0이면_생성하지_않는다(final int strength) {
        assertThatThrownBy(() -> memory(UserMemoryType.TOPIC, "추리 소설", strength, List.of(0.1f, 0.2f), 2)).isInstanceOf(CoreException.class);
    }

    @Test
    void 벡터_길이가_dim과_다르면_생성하지_않는다() {
        assertThatThrownBy(() -> memory(UserMemoryType.TOPIC, "추리 소설", 2, List.of(0.1f), 2)).isInstanceOf(CoreException.class);
    }

    @Test
    void 종류가_같고_정규화한_핵심어가_같으면_같은_기억으로_본다() {
        final UserMemory memory = memory(UserMemoryType.AUTHOR, " Haruki  Murakami ", 2, List.of(0.1f, 0.2f), 2);

        assertThat(memory.hasSameKeywordAs(memory(UserMemoryType.AUTHOR, "haruki murakami", -1, List.of(0.3f, 0.4f), 2))).isTrue();
        assertThat(memory.hasSameKeywordAs(memory(UserMemoryType.TOPIC, "haruki murakami", 2, List.of(0.1f, 0.2f), 2))).isFalse();
    }

    private UserMemory memory(final UserMemoryType type, final String keyword, final int strength, final List<Float> vector,
        final int dim) {
        return new UserMemory(1L, type, "기억 문장", strength, List.of(), keyword, vector, dim, "cv_1");
    }
}
