package com.book.core.support.api;

import com.book.common.response.ApiResponse;
import com.book.core.support.api.spec.SupportControllerSpec;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class SupportController implements SupportControllerSpec {
    @Override
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Void>> checkHealth() {
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
