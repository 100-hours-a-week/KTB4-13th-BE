package com.book.core.coupon.api;

import com.book.common.response.ApiResponse;
import com.book.core.coupon.api.response.CacheMetricsResponse;
import com.book.core.coupon.application.service.CacheMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/metrics/cache")
@RequiredArgsConstructor
public class CacheMetricsController {
    private final CacheMetrics cacheMetrics;

    @GetMapping
    public ResponseEntity<ApiResponse<CacheMetricsResponse>> snapshot() {
        return ResponseEntity.ok(ApiResponse.ok(CacheMetricsResponse.from(cacheMetrics.snapshot())));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<Void>> reset() {
        cacheMetrics.reset();
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
