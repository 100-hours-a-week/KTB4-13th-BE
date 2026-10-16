package com.book.core.cancel.api;

import com.book.common.config.security.UserId;
import com.book.common.response.ApiResponse;
import com.book.core.cancel.api.converter.CancelCommandConverter;
import com.book.core.cancel.api.request.CancelRequest;
import com.book.core.cancel.api.spec.CancelControllerSpec;
import com.book.core.cancel.application.service.CancelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class CancelController implements CancelControllerSpec {
    private final CancelService cancelService;
    private final CancelCommandConverter commandConverter;

    @Override
    @PostMapping("/api/v1/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(@UserId final Long userId, @Valid @RequestBody final CancelRequest request) {
        cancelService.cancel(commandConverter.toCancelCommand(userId, request));
        return ResponseEntity.ok(ApiResponse.ok());
    }

}
