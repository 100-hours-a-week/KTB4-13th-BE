package com.book.core.coupon.api;

import com.book.common.config.security.UserId;
import com.book.common.response.ApiResponse;
import com.book.core.coupon.api.converter.CouponCommandConverter;
import com.book.core.coupon.api.response.WaitingRoomResponse;
import com.book.core.coupon.api.spec.WaitingRoomControllerSpec;
import com.book.core.coupon.application.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/waiting-room")
@RequiredArgsConstructor
public class WaitingRoomController implements WaitingRoomControllerSpec {
    private final CouponService couponService;
    private final CouponCommandConverter commandConverter;

    @Override
    @PostMapping("/{couponId}")
    public ApiResponse<WaitingRoomResponse> enter(@PathVariable final long couponId, @UserId final Long userId) {
        return ApiResponse
            .ok(WaitingRoomResponse.from(couponService.enterWaitingRoom(commandConverter.toIssueCouponCommand(userId, couponId))));
    }

    @Override
    @GetMapping("/{couponId}")
    public ApiResponse<WaitingRoomResponse> status(@PathVariable final long couponId, @UserId final Long userId) {
        return ApiResponse
            .ok(WaitingRoomResponse.from(couponService.getWaitingRoomStatus(commandConverter.toIssueCouponCommand(userId, couponId))));
    }
}
