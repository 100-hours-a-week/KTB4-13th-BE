package com.book.core.agentchat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CartChangeLineTest {
    @Test
    void 담기는_미담김_기준과_대체할_수량을_보관한다() {
        final CartChangeLine line = new CartChangeLine(CartChangeOperation.ADD, 20L, 0, 2, new BigDecimal("18000"));
        assertThat(line.expectedQuantity()).isZero();
        assertThat(line.quantity()).isEqualTo(2);
        assertThat(line.expectedUnitPrice()).isEqualByComparingTo("18000");
    }

    @Test
    void 삭제는_새_수량과_가격을_요구하지_않는다() {
        final CartChangeLine line = new CartChangeLine(CartChangeOperation.REMOVE, 20L, 2, null, null);
        assertThat(line.quantity()).isNull();
        assertThat(line.expectedUnitPrice()).isNull();
    }

    @Test
    void 수정은_기존_항목과_유효한_수량을_요구한다() {
        assertThatThrownBy(() -> new CartChangeLine(CartChangeOperation.UPDATE, 20L, 0, 2, BigDecimal.TEN))
            .isInstanceOf(CoreException.class);
    }
}
