package com.book.core.coupon.api;

import com.book.common.config.security.UserId;
import com.book.common.response.ApiResponse;
import com.book.core.coupon.api.converter.CouponCommandConverter;
import com.book.core.coupon.api.converter.CouponResultConverter;
import com.book.core.coupon.api.request.CreateCouponRequest;
import com.book.core.coupon.api.response.CouponResponse;
import com.book.core.coupon.api.response.OwnedCouponResponse;
import com.book.core.coupon.api.spec.CouponControllerSpec;
import com.book.core.coupon.application.command.CreateCouponCommand;
import com.book.core.coupon.application.command.GetCouponsCommand;
import com.book.core.coupon.application.command.GetOwnedCouponsCommand;
import com.book.core.coupon.application.command.IssueCouponCommand;
import com.book.core.coupon.application.result.CouponResult;
import com.book.core.coupon.application.result.OwnedCouponResult;
import com.book.core.coupon.application.service.CouponService;
import com.book.core.coupon.application.service.OwnedCouponService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
public class CouponController implements CouponControllerSpec {
    private final CouponService couponService;
    private final OwnedCouponService ownedCouponService;
    private final CouponCommandConverter commandConverter;
    private final CouponResultConverter resultConverter;

    @Override
    @PostMapping("/admin-api/v1/coupons")
    public ResponseEntity<ApiResponse<CouponResponse>> create(@Valid @RequestBody final CreateCouponRequest request) {
        final CreateCouponCommand command = commandConverter.toCreateCouponCommand(request);
        final CouponResult result = couponService.createCoupon(command);
        return ResponseEntity.ok(ApiResponse.ok(resultConverter.toCouponResponse(result)));
    }

    @Override
    @GetMapping("/api/v1/coupons")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getCoupons(@RequestParam("ids") final List<Long> couponIds) {
        final GetCouponsCommand command = commandConverter.toGetCouponsCommand(couponIds);
        final List<CouponResult> result = couponService.getCoupons(command);
        final List<CouponResponse> response = resultConverter.toCouponResponses(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @PostMapping("/api/v1/coupons/{couponId}/issue")
    public ResponseEntity<ApiResponse<Void>> issue(@UserId final Long userId, @PathVariable final Long couponId) {
        final IssueCouponCommand command = commandConverter.toIssueCouponCommand(userId, couponId);
        couponService.issue(command);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    @GetMapping("/api/v1/owned-coupons")
    public ResponseEntity<ApiResponse<List<OwnedCouponResponse>>> getOwnedCoupons(@UserId final Long userId) {
        final GetOwnedCouponsCommand command = commandConverter.toGetOwnedCouponsCommand(userId);
        final List<OwnedCouponResult> result = ownedCouponService.getOwnedCoupons(command);
        final List<OwnedCouponResponse> response = resultConverter.toOwnedCouponResponses(result);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
