package com.book.core.cart.api.spec;

import com.book.core.cart.api.request.CartAddRequest;
import com.book.core.cart.api.request.CartChangeQuantityRequest;
import com.book.core.cart.api.request.CartDeleteRequest;
import com.book.core.cart.application.result.CartQueryResult;
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
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Cart", description = "회원 장바구니 API")
public interface CartControllerSpec {
    @Operation(summary = "내 장바구니 조회")
    @ApiResponses(@ApiResponse(responseCode = "200", description = "조회 성공"))
    com.book.common.response.ApiResponse<CartQueryResult> query(
            @Parameter(in = ParameterIn.QUERY, required = true, example = "42") @Positive @RequestParam("userId")
                    final long userId);

    @Operation(
            summary = "장바구니 상품 추가",
            description = "동일 장바구니에 같은 상품이 있으면 기존 수량에 누적하지 않고 "
                    + "요청한 수량으로 대체합니다. 같은 요청을 반복해도 결과는 같습니다. "
                    + "quantity가 0 이하이면 요청을 거부합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "추가 성공"),
        @ApiResponse(responseCode = "400", description = "요청 오류")
    })
    ResponseEntity<com.book.common.response.ApiResponse<Void>> add(
            @Parameter(in = ParameterIn.QUERY, required = true, example = "42") @Positive @RequestParam("userId")
                    final long userId,
            @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CartAddRequest.class)))
                    @Valid
                    final CartAddRequest request);

    @Operation(
            summary = "장바구니 상품 수량 변경",
            description = "기존 수량을 요청한 quantity로 대체합니다. quantity가 1보다 작으면 " + "1로 보정하고, 500보다 크면 요청을 거부합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수량 변경 성공"),
        @ApiResponse(responseCode = "400", description = "요청 오류")
    })
    ResponseEntity<com.book.common.response.ApiResponse<Void>> changeQuantity(
            @Parameter(in = ParameterIn.QUERY, required = true, example = "42") @Positive @RequestParam("userId")
                    final long userId,
            @Parameter(in = ParameterIn.PATH, required = true) @Positive final long cartItemId,
            @RequestBody(
                            required = true,
                            content = @Content(schema = @Schema(implementation = CartChangeQuantityRequest.class)))
                    @Valid
                    final CartChangeQuantityRequest request);

    @Operation(summary = "장바구니 상품 삭제")
    ResponseEntity<com.book.common.response.ApiResponse<Void>> delete(
            @Parameter(in = ParameterIn.QUERY, required = true, example = "42") @Positive @RequestParam("userId")
                    final long userId,
            @Parameter(in = ParameterIn.PATH, required = true) @Positive final long cartItemId);

    @Operation(summary = "장바구니 상품 다건 삭제")
    ResponseEntity<com.book.common.response.ApiResponse<Void>> deleteMany(
            @Parameter(in = ParameterIn.QUERY, required = true, example = "42") @Positive @RequestParam("userId")
                    final long userId,
            @RequestBody(
                            required = true,
                            content = @Content(schema = @Schema(implementation = CartDeleteRequest.class)))
                    @Valid
                    final CartDeleteRequest request);
}
