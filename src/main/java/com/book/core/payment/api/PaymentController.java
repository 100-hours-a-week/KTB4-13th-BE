package com.book.core.payment.api;

import com.book.common.config.security.UserId;
import com.book.common.response.ApiResponse;
import com.book.core.payment.api.converter.PaymentCommandConverter;
import com.book.core.payment.api.request.CreatePaymentRequest;
import com.book.core.payment.api.response.CreatePaymentResponse;
import com.book.core.payment.api.spec.PaymentControllerSpec;
import com.book.core.payment.application.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
class PaymentController implements PaymentControllerSpec {
    private final PaymentService paymentService;
    private final PaymentCommandConverter commandConverter;

    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<CreatePaymentResponse>> create(@UserId final Long userId,
        @Valid @RequestBody final CreatePaymentRequest request) {
        return ResponseEntity
            .ok(ApiResponse.ok(new CreatePaymentResponse(paymentService.create(commandConverter.toCreateCommand(userId, request)))));
    }

    @Override
    @PostMapping("/callback/success")
    public ResponseEntity<ApiResponse<Void>> callbackForSuccess(@NotBlank @Size(max = 255) @RequestParam("orderId") final String orderKey,
        @NotBlank @Size(max = 255) @RequestParam("paymentKey") final String paymentKey, @RequestParam("amount") final BigDecimal amount) {
        paymentService.success(commandConverter.toApproveCommand(orderKey, paymentKey, amount));
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    @PostMapping("/callback/fail")
    public ResponseEntity<ApiResponse<Void>> callbackForFail(@NotBlank @Size(max = 255) @RequestParam("orderId") final String orderId,
        @RequestParam("code") final String code, @RequestParam("message") final String message) {
        paymentService.fail(commandConverter.toFailCommand(orderId, code, message));
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
