package com.book.core.user.infrastructure.nickname;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.book.core.user.domain.User;
import org.junit.jupiter.api.Test;

class RandomNicknameGeneratorTest {
    private final RandomNicknameGenerator generator = new RandomNicknameGenerator();

    @Test
    void 한글_단어와_숫자_suffix를_조합해_User_규칙을_만족하는_후보를_생성한다() {
        for (int count = 0; count < 100; count++) {
            final String nickname = generator.generate();
            assertThat(nickname).isNotBlank().matches("^[가-힣]+\\d{4}$").hasSizeBetween(2, 20);
            assertThatCode(() -> User.create(nickname)).doesNotThrowAnyException();
        }
    }
}
