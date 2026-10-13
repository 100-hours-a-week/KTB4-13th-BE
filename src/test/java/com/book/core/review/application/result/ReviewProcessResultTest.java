package com.book.core.review.application.result;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.review.domain.ReviewFormat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReviewProcessResultTest {
    @ParameterizedTest
    @CsvSource({"TEXT, 1000", "IMAGE, 1500"})
    void 리뷰_형식에_따른_포인트_금액을_반환한다(final ReviewFormat format, final String amount) {
        assertThat(new ReviewProcessResult(801L, format).pointAmount()).isEqualByComparingTo(amount);
    }
}
