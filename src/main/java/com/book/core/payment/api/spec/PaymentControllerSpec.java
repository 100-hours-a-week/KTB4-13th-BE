package com.book.core.payment.api.spec;

import com.book.common.response.ApiResponse;
import com.book.core.payment.api.request.CreatePaymentRequest;
import com.book.core.payment.api.response.CreatePaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

/** 참고 결제 API를 Book의 /api/v1 경로와 공통 응답 형식으로 제공합니다. */
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payment", description = "일반 결제")
public interface PaymentControllerSpec {
    @Operation(summary = "일반 주문 결제 생성", description = "usePoint 미입력 시 참고의 -1 계산을 유지합니다. PG 승인은 아직 연동되지 않았습니다.")
    ResponseEntity<ApiResponse<CreatePaymentResponse>> create(@Parameter(hidden = true) final Long userId,
        @Valid final CreatePaymentRequest request);

}
