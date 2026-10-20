package com.book.core.notification.api.spec;

import com.book.common.response.ApiResponse;
import com.book.core.notification.api.response.NotificationListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Notification", description = "회원 알림 API")
@SecurityRequirement(name = "bearerAuth")
public interface NotificationControllerSpec {
    @Operation(summary = "최근 알림 조회",
        description = "본인의 활성 알림 중 최신 ID 순으로 최대 20개와 전체 안 읽은 개수를 즉시 반환합니다. " + "숏 폴링 주기는 클라이언트에서 제어하며 목록 조회는 읽음 상태를 변경하지 않습니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 목록과 전체 미읽음 개수"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효한 인증 필요")})
    ResponseEntity<ApiResponse<NotificationListResponse>> getRecent(@Parameter(hidden = true) final Long userId);

    @Operation(summary = "개별 알림 읽음 처리", description = "본인 활성 알림만 읽음 처리하며 반복 요청은 최초 읽음 시각을 유지합니다.")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공 또는 이미 읽음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "알림 ID는 양수여야 함"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효한 인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알림 없음 또는 다른 회원의 알림")})
    ResponseEntity<ApiResponse<Void>> read(@Parameter(hidden = true) final Long userId,
        @Positive @PathVariable("notificationId") final Long notificationId);
}
