package com.book.core.coupon.api;

import com.book.common.response.ApiResponse;
import com.book.core.coupon.api.converter.IssuanceDltCommandConverter;
import com.book.core.coupon.api.request.ReplayIssuanceDltRequest;
import com.book.core.coupon.api.spec.IssuanceDltControllerSpec;
import com.book.core.coupon.application.service.IssuanceDltService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "coupon.kafka.worker-enabled", matchIfMissing = true)
public class IssuanceDltController implements IssuanceDltControllerSpec {
    private final IssuanceDltService service;
    private final IssuanceDltCommandConverter converter;

    @Override
    @PostMapping("/admin-api/v1/coupons/issuance-dlt/replay")
    public ApiResponse<Integer> replay(@Valid @RequestBody final ReplayIssuanceDltRequest request) {
        return ApiResponse.ok(service.replay(converter.toCommand(request)));
    }
}
