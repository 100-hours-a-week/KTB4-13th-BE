package com.book.core.product.application.usecase;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetProductsForCartChangeUseCaseTest {
    @Test
    void 잘못된_상품_ID는_저장소_호출_전에_거절한다() {
        final GetProductsForCartChangeUseCase useCase = new GetProductsForCartChangeUseCase((final List<Long> ids) -> {
            throw new AssertionError("잘못된 입력으로 저장소를 호출했습니다.");
        });

        assertInvalid(useCase, null);
        assertInvalid(useCase, Arrays.asList(1L, null));
        assertInvalid(useCase, List.of(0L));
        assertInvalid(useCase, List.of(-1L));
    }

    private static void assertInvalid(final GetProductsForCartChangeUseCase useCase, final List<Long> productIds) {
        assertThatThrownBy(() -> useCase.execute(productIds)).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

}
