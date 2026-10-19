package com.book.core.user.api;

import com.book.common.config.security.UserId;
import com.book.common.response.ApiResponse;
import com.book.core.auth.api.cookie.RefreshTokenCookieFactory;
import com.book.core.user.api.converter.UserCommandConverter;
import com.book.core.user.api.request.UpdateMyInfoRequest;
import com.book.core.user.api.response.GetMyInfoResponse;
import com.book.core.user.api.spec.UserControllerSpec;
import com.book.core.user.application.result.GetMyInfoResult;
import com.book.core.user.application.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
class UserController implements UserControllerSpec {
    private final UserService userService;
    private final UserCommandConverter commandConverter;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    @Override
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<GetMyInfoResponse>> getMyInfo(@UserId final Long userId) {
        final GetMyInfoResult result = userService.getMyInfo(userId);
        return ResponseEntity.ok(ApiResponse.ok(new GetMyInfoResponse(result.nickname())));
    }

    @Override
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<Void>> updateMyInfo(@UserId final Long userId,
        @Valid @RequestBody final UpdateMyInfoRequest request) {
        userService.updateMyInfo(commandConverter.toUpdateMyInfoCommand(userId, request));
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<Void>> requestUserDeletion(@UserId final Long userId) {
        userService.requestUserDeletion(userId);
        final String expiredRefreshCookie = refreshTokenCookieFactory.expire().toString();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, expiredRefreshCookie).body(ApiResponse.ok());
    }
}
