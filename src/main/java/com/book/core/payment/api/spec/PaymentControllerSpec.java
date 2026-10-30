package com.book.core.payment.api.spec;

import com.book.common.response.ApiResponse;
import com.book.core.payment.api.request.CreatePaymentRequest;
import com.book.core.payment.api.response.CreatePaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payment", description = "일반 결제")
public interface PaymentControllerSpec {
    @Operation(summary = "일반 주문 결제 생성",
        description = "Idempotency-Key는 재전송 시 재사용하고 새 결제 시도에는 새 키를 사용합니다. 반환된 pgOrderId를 토스 orderId로 사용합니다.")
    ResponseEntity<ApiResponse<CreatePaymentResponse>> create(@Parameter(hidden = true) final Long userId,
        @Valid final CreatePaymentRequest request,
        @RequestHeader("Idempotency-Key") @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,100}") final String requestKey);

    @Operation(summary = "결제 성공 콜백", description = "orderKey에 결제 생성 응답의 pgOrderId를 전달합니다. 0원 결제는 paymentKey를 생략할 수 있습니다.")
    ResponseEntity<ApiResponse<Void>> callbackForSuccess(@Parameter(hidden = true) final Long userId,
        @NotBlank @Size(max = 255) final String orderKey, @Size(max = 200) final String paymentKey, final BigDecimal amount);

    @Operation(summary = "결제 실패 콜백", description = "orderKey에 결제 생성 응답의 pgOrderId를 전달합니다. 양수 결제와 진행 중인 작업은 결과 확인 중으로 응답합니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "0원 결제 실패 및 예약 해제 완료"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "결제 결과 확인 중 (PG_PENDING)")})
    ResponseEntity<ApiResponse<Void>> callbackForFail(@Parameter(hidden = true) final Long userId,
        @NotBlank @Size(max = 255) final String orderKey, final String code, final String message);
}
