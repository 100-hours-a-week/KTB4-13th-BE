package com.book.core.notification.api.spec;

import com.book.common.response.ApiResponse;
import com.book.core.notification.api.response.NotificationListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Notification", description = "회원 알림 API")
@SecurityRequirement(name = "bearerAuth")
public interface NotificationControllerSpec {
    @Operation(summary = "최근 알림 조회",
        description = "본인의 활성 알림 중 최신 ID 순으로 최대 20개와 전체 안 읽은 개수를 즉시 반환합니다. " + "숏 폴링 주기는 클라이언트에서 제어하며 목록 조회는 읽음 상태를 변경하지 않습니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 목록과 전체 미읽음 개수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효한 인증 필요")})
    ResponseEntity<ApiResponse<NotificationListResponse>> getRecent(@Parameter(hidden = true) final Long userId);
}
