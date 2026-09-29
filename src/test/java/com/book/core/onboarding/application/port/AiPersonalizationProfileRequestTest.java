package com.book.core.onboarding.application.port;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AiPersonalizationProfileRequestTest {
    @Test
    void 같은_요청_내용이면_같은_멱등_키를_만든다() {
        assertThat(request(List.of("소설"), List.of("SF")).idempotencyKey())
            .isEqualTo(request(List.of("소설"), List.of("SF")).idempotencyKey());
    }

    @Test
    void 요청_내용이_다르면_다른_멱등_키를_만든다() {
        assertThat(request(List.of("소설"), List.of("SF")).idempotencyKey())
            .isNotEqualTo(request(List.of("소설"), List.of("추리/스릴러")).idempotencyKey());
    }

    @Test
    void 값이_다른_필드로_옮겨가면_다른_멱등_키를_만든다() {
        assertThat(request(List.of("소설"), List.of()).idempotencyKey()).isNotEqualTo(request(List.of(), List.of("소설")).idempotencyKey());
    }

    @Test
    void 멱등_키는_사용자_ID와_SHA_256_값으로_구성된다() {
        assertThat(request(List.of("소설"), List.of("SF")).idempotencyKey()).matches("profile:42:[0-9a-f]{64}");
    }

    private static AiPersonalizationProfileRequest request(final List<String> categories, final List<String> tags) {
        return new AiPersonalizationProfileRequest(42L, List.of("잠들기 전"), List.of("베스트셀러"), categories, tags, List.of(100L, 300L));
    }
}
