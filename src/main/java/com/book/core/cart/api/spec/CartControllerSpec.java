package com.book.core.cart.api.spec;

import com.book.core.cart.api.request.AddCartItemRequest;
import com.book.core.cart.api.request.ModifyCartItemRequest;
import com.book.core.cart.api.response.CartResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Cart", description = "장바구니 API")
public interface CartControllerSpec {
    @Operation(
            summary = "장바구니 상품 추가",
            description = "동일 장바구니에 같은 상품이 있으면 기존 수량에 누적하지 않고 요청한 수량으로 대체합니다. "
                    + "같은 추가 요청을 반복해도 결과는 같습니다. quantity는 1~500 범위로 필수 입력입니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "추가 성공"),
        @ApiResponse(responseCode = "400", description = "수량 형식이 잘못되었거나 상품 재고가 부족함"),
        @ApiResponse(responseCode = "404", description = "장바구니 또는 상품을 찾을 수 없음")
    })
    ResponseEntity<com.book.common.response.ApiResponse<Void>> addCartItem(
            @Parameter(in = ParameterIn.QUERY, required = true, example = "42") @Positive @RequestParam("userId")
                    final Long userId,
            @RequestBody(
                            required = true,
                            content = @Content(schema = @Schema(implementation = AddCartItemRequest.class)))
                    @Valid
                    final AddCartItemRequest request);

    @Operation(summary = "장바구니 상품 수량 변경", description = "요청 회원의 활성 장바구니 상품 수량을 변경합니다. 재고가 부족하면 변경하지 않습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수량 변경 성공"),
        @ApiResponse(responseCode = "400", description = "수량 형식이 잘못되었거나 상품 재고가 부족함"),
        @ApiResponse(responseCode = "404", description = "장바구니 상품 또는 상품을 찾을 수 없음")
    })
    ResponseEntity<com.book.common.response.ApiResponse<Void>> modifyCartItem(
            @Parameter(in = ParameterIn.QUERY, required = true, example = "42") @Positive @RequestParam("userId")
                    final Long userId,
            @Parameter(in = ParameterIn.PATH, required = true, example = "11") @Positive @PathVariable("cartItemId")
                    final Long cartItemId,
            @RequestBody(
                            required = true,
                            content = @Content(schema = @Schema(implementation = ModifyCartItemRequest.class)))
                    @Valid
                    final ModifyCartItemRequest request);

    @Operation(summary = "장바구니 조회", description = "userId에 해당하는 활성 장바구니 항목을 최근 추가순으로 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "장바구니 조회 성공"),
        @ApiResponse(responseCode = "400", description = "userId가 없거나 양수가 아님"),
        @ApiResponse(responseCode = "500", description = "장바구니 조회 실패")
    })
    ResponseEntity<com.book.common.response.ApiResponse<CartResponse>> getCart(
            @Parameter(in = ParameterIn.QUERY, required = true, example = "42") @Positive @RequestParam("userId")
                    final Long userId);
}
