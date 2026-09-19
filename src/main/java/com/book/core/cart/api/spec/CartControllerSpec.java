package com.book.core.cart.api.spec;

import com.book.core.cart.api.request.AddCartItemRequest;
import com.book.core.cart.api.response.CartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.security.Principal;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Cart", description = "장바구니 API")
public interface CartControllerSpec {
    @Operation(
            summary = "장바구니 상품 추가",
            description = "동일 장바구니에 같은 상품이 있으면 기존 수량에 누적하지 않고 요청한 수량으로 대체합니다. "
                    + "같은 추가 요청을 반복해도 결과는 같습니다. quantity는 1~500 범위로 필수 입력입니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "추가 성공"),
        @ApiResponse(responseCode = "400", description = "요청 오류")
    })
    ResponseEntity<com.book.common.response.ApiResponse<Void>> addCartItem(
            @Parameter(in = ParameterIn.QUERY, required = true, example = "42") @Positive @RequestParam("userId")
                    final Long userId,
            @RequestBody(
                            required = true,
                            content = @Content(schema = @Schema(implementation = AddCartItemRequest.class)))
                    @Valid
                    final AddCartItemRequest request);

    @Operation(summary = "장바구니 조회", description = "인증된 회원의 활성 장바구니 항목을 최근 추가순으로 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "장바구니 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 없거나 유효하지 않음"),
        @ApiResponse(responseCode = "500", description = "장바구니 조회 실패")
    })
    ResponseEntity<com.book.common.response.ApiResponse<CartResponse>> getCart(
            @Parameter(in = ParameterIn.HEADER, required = true, example = "Bearer access-token")
                    @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
                    final String authorization,
            @Parameter(hidden = true) final Principal principal);
}
