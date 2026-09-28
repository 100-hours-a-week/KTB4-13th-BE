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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@Tag(name = "Cart", description = "장바구니 API")
public interface CartControllerSpec {
    @Operation(summary = "장바구니 상품 추가",
        description = "동일 장바구니에 같은 상품이 있으면 기존 수량에 누적하지 않고 요청한 수량으로 대체합니다. " + "같은 추가 요청을 반복해도 결과는 같습니다. quantity는 1~500 범위로 필수 입력입니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "추가 성공"), @ApiResponse(responseCode = "400", description = "요청 오류"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<Void>> addCartItem(@AuthenticationPrincipal final Jwt jwt,
        @RequestBody(required = true, content = @Content(schema = @Schema(implementation = AddCartItemRequest.class)))
        @Valid final AddCartItemRequest request);

    @Operation(summary = "장바구니 조회", description = "인증된 회원의 활성 장바구니 항목을 최근 추가순으로 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "장바구니 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"), @ApiResponse(responseCode = "500", description = "장바구니 조회 실패")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal final Jwt jwt);
}
