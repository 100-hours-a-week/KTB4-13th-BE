package com.book.core.review.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.paging.OffsetLimit;
import org.junit.jupiter.api.Test;

class ReviewTargetTest {
    @Test
    void 페이징_검증은_공통_VO에서_유지된다() {
        assertThatThrownBy(() -> new OffsetLimit(-1, 20)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new OffsetLimit(0, 0)).isInstanceOf(CoreException.class);
    }
}
