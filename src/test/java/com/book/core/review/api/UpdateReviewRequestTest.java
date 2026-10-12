package com.book.core.review.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.core.review.api.request.UpdateReviewRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UpdateReviewRequestTest {
    @ParameterizedTest
    @ValueSource(strings = {"0", "-0.5", "5.5"})
    void 범위를_벗어난_별점을_거부한다(final String rate) {
        final var request = new UpdateReviewRequest(new BigDecimal(rate), "본문", null);
        assertThatThrownBy(request::toContent).isInstanceOf(CoreException.class);
    }

    @Test
    void 유효한_별점과_본문을_VO로_변환한다() {
        final var content = new UpdateReviewRequest(new BigDecimal("4.50"), "본문", null).toContent();
        assertThat(content.rate()).isEqualByComparingTo("4.5");
        assertThat(content.content()).isEqualTo("본문");
    }
}
