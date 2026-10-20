package com.book.core.notification.api;

import com.book.common.config.security.UserId;
import com.book.common.response.ApiResponse;
import com.book.core.notification.api.converter.NotificationCommandConverter;
import com.book.core.notification.api.converter.NotificationResultConverter;
import com.book.core.notification.api.response.NotificationListResponse;
import com.book.core.notification.api.spec.NotificationControllerSpec;
import com.book.core.notification.application.service.NotificationService;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
class NotificationController implements NotificationControllerSpec {
    private final NotificationService notificationService;
    private final NotificationCommandConverter commandConverter;
    private final NotificationResultConverter resultConverter;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationListResponse>> getRecent(@UserId final Long userId) {
        final var result = notificationService.getRecent(commandConverter.toGetCommand(userId));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.ok(resultConverter.toResponse(result)));
    }

    @Override
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> read(@UserId final Long userId,
        @Positive @PathVariable("notificationId") final Long notificationId) {
        notificationService.read(commandConverter.toReadCommand(userId, notificationId));
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
