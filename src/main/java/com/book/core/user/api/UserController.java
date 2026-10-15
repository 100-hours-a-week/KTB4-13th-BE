package com.book.core.user.api;

import com.book.common.config.security.UserId;
import com.book.common.response.ApiResponse;
import com.book.core.user.api.response.GetMyInfoResponse;
import com.book.core.user.api.spec.UserControllerSpec;
import com.book.core.user.application.result.GetMyInfoResult;
import com.book.core.user.application.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
class UserController implements UserControllerSpec {
    private final UserService userService;

    @Override
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<GetMyInfoResponse>> getMyInfo(@UserId final Long userId) {
        final GetMyInfoResult result = userService.getMyInfo(userId);
        return ResponseEntity.ok(ApiResponse.ok(new GetMyInfoResponse(result.nickname())));
    }
}
