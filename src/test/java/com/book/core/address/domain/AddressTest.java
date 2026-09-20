package com.book.core.address.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class AddressTest {
    @Test
    void 주소지를_생성하면_값을_정규화하고_ACTIVE_상태로_시작한다() {
        final var address = Address.of(1L, "  집  ", " 12345 ", " 서울시 강남구 ", "   ", false);

        assertThat(address.userId()).isEqualTo(1L);
        assertThat(address.label()).isEqualTo("집");
        assertThat(address.postalCode()).isEqualTo("12345");
        assertThat(address.address()).isEqualTo("서울시 강남구");
        assertThat(address.detailAddress()).isNull();
        assertThat(address.isActive()).isTrue();
        assertThat(address.isDefaultAddress()).isFalse();
    }

    @Test
    void 필수_주소지_값이_공백이면_생성하지_않는다() {
        assertThatThrownBy(() -> Address.of(1L, " ", "12345", "서울시 강남구", null, false))
                .isInstanceOfSatisfying(
                        CoreException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void 상세_주소가_있으면_앞뒤_공백을_제거한다() {
        final var address = Address.of(1L, "집", "12345", "서울시 강남구", "  101호  ", false);

        assertThat(address.detailAddress()).isEqualTo("101호");
    }

    @Test
    void 기본_배송지_상태를_전환한다() {
        final var address = Address.of(1L, "집", "12345", "서울시 강남구", null, false);

        address.setDefaultAddress();
        assertThat(address.isDefaultAddress()).isTrue();

        address.unsetDefaultAddress();
        assertThat(address.isDefaultAddress()).isFalse();
    }
}
