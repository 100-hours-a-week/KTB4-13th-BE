package com.book.core.coupon.application.command;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class IssuanceRequestedTest {
    @Test
    void 기존_발급_요청_JSON을_읽고_같은_필드로_쓴다() {
        final var mapper = JsonMapper.builder().build();
        final String payload = """
            {"couponId":10,"userId":42,"maxUseCount":2}
            """;

        final var request = mapper.readValue(payload, IssuanceRequested.class);

        assertThat(request).isEqualTo(new IssuanceRequested(10L, 42L, 2L));
        assertThat(mapper.readTree(mapper.writeValueAsString(request))).isEqualTo(mapper.readTree(payload));
    }
}
