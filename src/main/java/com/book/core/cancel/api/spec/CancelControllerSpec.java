package com.book.core.cancel.api.spec;

import com.book.common.response.ApiResponse;
import com.book.core.cancel.api.request.CancelRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "Cancel", description = "결제 완료 주문의 취소")
@SecurityRequirement(name = "bearerAuth")
public interface CancelControllerSpec {
    @Operation(summary = "결제 전체 취소", description = "PAID 주문만 허용합니다. 실제 PG 취소 연동은 참고처럼 미구현입니다.")
    ResponseEntity<ApiResponse<Void>> cancel(@Parameter(hidden = true) final Long userId, @Valid final CancelRequest request);

}
