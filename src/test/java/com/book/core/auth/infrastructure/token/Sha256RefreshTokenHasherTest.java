package com.book.core.auth.infrastructure.token;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class Sha256RefreshTokenHasherTest {
    private final Sha256RefreshTokenHasher hasher = new Sha256RefreshTokenHasher();

    @Test
    void 같은_token은_같은_hash를_생성한다() {
        assertThat(hasher.hash("refresh-token")).isEqualTo(hasher.hash("refresh-token"));
    }

    @Test
    void 다른_token은_다른_hash를_생성한다() {
        assertThat(hasher.hash("refresh-token-1")).isNotEqualTo(hasher.hash("refresh-token-2"));
    }

    @Test
    void hash는_64자리_16진수이며_token_원문을_포함하지_않는다() {
        final String refreshToken = "refresh-token";

        final String tokenHash = hasher.hash(refreshToken);

        assertThat(tokenHash).hasSize(64).matches("[0-9a-f]{64}").doesNotContain(refreshToken);
    }
}
