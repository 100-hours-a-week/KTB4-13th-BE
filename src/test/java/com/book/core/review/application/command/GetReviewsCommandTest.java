package com.book.core.review.application.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.paging.OffsetLimit;
import com.book.core.review.domain.enums.ReviewTargetType;
import org.junit.jupiter.api.Test;

class GetReviewsCommandTest {
    @Test
    void 잘못된_대상과_페이징을_거부한다() {
        final var paging = new OffsetLimit(0, 20);
        assertThatThrownBy(() -> new GetReviewsCommand(null, 1L, paging)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new GetReviewsCommand(ReviewTargetType.PRODUCT, null, paging)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new GetReviewsCommand(ReviewTargetType.PRODUCT, 0L, paging)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new GetReviewsCommand(ReviewTargetType.PRODUCT, 1L, null)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new OffsetLimit(-1, 20)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new OffsetLimit(0, 0)).isInstanceOf(CoreException.class);
    }
}
