package com.book.core.payment.api;

import com.book.common.config.security.UserId;
import com.book.common.response.ApiResponse;
import com.book.core.payment.api.converter.PaymentCommandConverter;
import com.book.core.payment.api.request.CreatePaymentRequest;
import com.book.core.payment.api.response.CreatePaymentResponse;
import com.book.core.payment.api.spec.PaymentControllerSpec;
import com.book.core.payment.application.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

}
