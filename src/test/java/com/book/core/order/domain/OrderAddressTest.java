package com.book.core.order.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import org.junit.jupiter.api.Test;

class OrderAddressTest {
    @Test
    void 다섯_자_이하_우편번호와_주문_주소를_스냅샷한다() {
        final OrderAddress address = OrderAddress.from("0A236", "서울 주소", null);

        assertThat(address.postalCode()).isEqualTo("0A236");
        assertThat(address.address()).isEqualTo("서울 주소");
        assertThat(address.detailAddress()).isNull();
    }

    @Test
    void 우편번호가_다섯_자를_초과하면_거부한다() {
        assertThatThrownBy(() -> OrderAddress.from("123456", "서울 주소", null)).isInstanceOf(CoreException.class);
    }
}
