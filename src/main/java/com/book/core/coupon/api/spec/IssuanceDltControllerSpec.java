package com.book.core.coupon.api.spec;

import com.book.common.api.RequiresSecurity;
import com.book.common.response.ApiResponse;
import com.book.core.coupon.api.request.ReplayIssuanceDltRequest;
import io.swagger.v3.oas.annotations.Operation;

public interface IssuanceDltControllerSpec {
    @RequiresSecurity
    @Operation(summary = "쿠폰 발급 실패 건 선별 재발행")
    ApiResponse<Integer> replay(ReplayIssuanceDltRequest request);
}
