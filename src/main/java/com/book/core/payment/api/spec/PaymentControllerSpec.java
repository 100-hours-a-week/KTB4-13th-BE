package com.book.core.payment.api.spec;

import com.book.common.response.ApiResponse;
import com.book.core.payment.api.request.CreatePaymentRequest;
import com.book.core.payment.api.response.CreatePaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.springframework.http.ResponseEntity;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payment", description = "일반 결제")
public interface PaymentControllerSpec {
    @Operation(summary = "일반 주문 결제 생성", description = "주문상품별 쿠폰과 사용 포인트를 입력합니다. usePoint 미입력은 0으로 처리하며, 승인 콜백에서 토스 결제를 확인합니다.")
    ResponseEntity<ApiResponse<CreatePaymentResponse>> create(@Parameter(hidden = true) final Long userId,
        @Valid final CreatePaymentRequest request);

    @Operation(summary = "결제 성공 콜백", description = "orderKey를 토스 orderId로 검증하고 카드 및 카드 기반 간편결제를 승인합니다. 0원 결제는 paymentKey를 생략할 수 있습니다.")
    ResponseEntity<ApiResponse<Void>> callbackForSuccess(@NotBlank @Size(max = 255) final String orderKey,
        @Size(max = 200) final String paymentKey, final BigDecimal amount);

    @Operation(summary = "결제 실패 콜백", description = "주문과 결제의 기존 상태를 변경하지 않고 실패 이력만 기록합니다.")
    ResponseEntity<ApiResponse<Void>> callbackForFail(@NotBlank @Size(max = 255) final String orderKey, final String code,
        final String message);
}
