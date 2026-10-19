package com.book.core.agentchat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.command.PrepareCartChangeCommand;
import com.book.core.agentchat.domain.CartChangeLine;
import com.book.core.agentchat.domain.CartChangeOperation;
import com.book.core.cart.application.command.CartItemChange;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PrepareCartChangeCommandTest {
    @Test
    void 저장_항목의_담기_수정_제거를_Cart_실행_입력으로_변환한다() {
        final List<CartChangeLine> stored = List.of(new CartChangeLine(CartChangeOperation.ADD, 20L, 0, 2, BigDecimal.TEN),
            new CartChangeLine(CartChangeOperation.UPDATE, 30L, 1, 3, BigDecimal.TEN),
            new CartChangeLine(CartChangeOperation.REMOVE, 40L, 2, null, null));
        final List<CartItemChange> items = PrepareCartChangeCommand.toCartItems(stored);
        assertThat(items).extracting("productId").containsExactly(20L, 30L, 40L);
        assertThat(items).extracting("quantity").containsExactly(2, 3, null);
        assertThat(items.getLast().isRemoval()).isTrue();
    }

    @Test
    void 잘못된_수량은_요청_준비_전에_기존_오류로_거절한다() {
        for (final int quantity : List.of(0, 501)) {
            final CartChangeLine line = new CartChangeLine(CartChangeOperation.ADD, 20L, 0, quantity, BigDecimal.TEN);
            assertThatThrownBy(() -> new PrepareCartChangeCommand(1L, "key", List.of(line))).isInstanceOfSatisfying(CoreException.class,
                (final CoreException error) -> assertThat(error.errorCode()).isEqualTo(ErrorCode.INVALID_CART_ITEM_QUANTITY));
        }
        final CartChangeLine line = new CartChangeLine(CartChangeOperation.UPDATE, 20L, 501, 2, BigDecimal.TEN);
        assertThatThrownBy(() -> new PrepareCartChangeCommand(1L, "key", List.of(line))).isInstanceOfSatisfying(CoreException.class,
            (final CoreException error) -> assertThat(error.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }
}
