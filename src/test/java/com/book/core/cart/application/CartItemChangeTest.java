package com.book.core.cart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.core.cart.application.command.ApplyCartChangesCommand;
import com.book.core.cart.application.command.CartItemChange;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CartItemChangeTest {
    @Test
    void 수량_대체와_제거를_저장_모델_없이_표현한다() {
        assertThat(new CartItemChange(20L, 0, 2, BigDecimal.TEN).isRemoval()).isFalse();
        assertThat(new CartItemChange(20L, 3, null, null).isRemoval()).isTrue();
    }

    @Test
    void 일괄변경_입력은_중복상품_잘못된_사용자_빈_항목을_거절한다() {
        final CartItemChange item = new CartItemChange(20L, 0, 2, BigDecimal.TEN);
        assertThatThrownBy(() -> new ApplyCartChangesCommand(1L, List.of(item, item), null)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new ApplyCartChangesCommand(0L, List.of(item), null)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new ApplyCartChangesCommand(1L, List.of(), null)).isInstanceOf(CoreException.class);
    }

    @Test
    void 수량_규칙과_제거의_전제조건을_실행_입력에서_검증한다() {
        assertThatThrownBy(() -> new CartItemChange(20L, 0, 501, BigDecimal.TEN)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new CartItemChange(20L, 501, 2, BigDecimal.TEN)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new CartItemChange(20L, 0, 0, BigDecimal.TEN)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new CartItemChange(20L, 0, null, null)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new CartItemChange(20L, 1, null, BigDecimal.TEN)).isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> new CartItemChange(20L, 0, 2, null)).isInstanceOf(CoreException.class);
    }
}
