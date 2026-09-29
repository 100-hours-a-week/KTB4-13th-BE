package com.book.core.cart.api.spec;

import com.book.core.cart.api.request.AddCartItemRequest;
import com.book.core.cart.api.request.DeleteCartItemsRequest;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Cart", description = "장바구니 API")
public interface CartControllerSpec {
    @Operation(summary = "장바구니 상품 추가",
        description = "동일 장바구니에 같은 상품이 있으면 기존 수량에 누적하지 않고 요청한 수량으로 대체합니다. " + "같은 추가 요청을 반복해도 결과는 같습니다. quantity는 1~500 범위로 필수 입력입니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "추가 성공"), @ApiResponse(responseCode = "400", description = "요청 오류"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<Void>> addCartItem(@Parameter(hidden = true) final Long userId,
        @RequestBody(required = true, content = @Content(schema = @Schema(implementation = AddCartItemRequest.class)))
        @Valid final AddCartItemRequest request);

    @Operation(summary = "장바구니 상품 수량 변경", description = "인증된 회원의 활성 장바구니 상품 수량을 변경합니다. 재고가 부족하면 변경하지 않습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "수량 변경 성공"),
        @ApiResponse(responseCode = "400", description = "수량 형식이 잘못되었거나 상품 재고가 부족함"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @ApiResponse(responseCode = "404", description = "장바구니 상품 또는 상품을 찾을 수 없음")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<Void>> modifyCartItem(@Parameter(hidden = true) final Long userId,
        @Parameter(in = ParameterIn.PATH, required = true, example = "11") @Positive @PathVariable("cartItemId") final Long cartItemId,
        @RequestBody(required = true, content = @Content(schema = @Schema(implementation = ModifyCartItemRequest.class)))
        @Valid final ModifyCartItemRequest request);

    @Operation(summary = "장바구니 상품 삭제", description = "인증된 회원의 활성 장바구니 상품을 논리 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "장바구니 상품 ID가 잘못됨"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @ApiResponse(responseCode = "404", description = "장바구니 상품을 찾을 수 없음")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<Void>> deleteCartItem(@Parameter(hidden = true) final Long userId,
        @Parameter(in = ParameterIn.PATH, required = true, example = "11") @Positive @PathVariable("cartItemId") final Long cartItemId);

    @Operation(summary = "장바구니 상품 다건 삭제", description = "인증된 회원의 활성 장바구니 상품들을 논리 삭제합니다. 대상이 하나라도 유효하지 않으면 전체 요청을 실패합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "요청 목록이 비었거나 장바구니 상품 ID가 잘못됨"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @ApiResponse(responseCode = "404", description = "장바구니 상품을 찾을 수 없음")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<Void>> deleteCartItems(@Parameter(hidden = true) final Long userId,
        @RequestBody(required = true, content = @Content(schema = @Schema(implementation = DeleteCartItemsRequest.class)))
        @Valid final DeleteCartItemsRequest request);

    @Operation(summary = "장바구니 조회", description = """
        인증된 회원의 활성 장바구니 항목을 최근 추가순으로 조회합니다. 항목에는 cartItemId, productId, itemName, thumbnailUrl,
        salePrice, discountedPrice, quantity, isAvailableForPurchase를 반환합니다. 상품 가격은 표시용 현재 가격이며 합계는 반환하지 않습니다.
        상품 또는 연결 도서가 삭제된 항목은 유지하고 이름과 표지를 반환하며 가격은 null, isAvailableForPurchase는 false로 반환합니다.
        상품 행이 없는 항목도 유지하며 상품 표시 정보는 null, isAvailableForPurchase는 false로 반환합니다.
        isAvailableForPurchase는 상품과 연결 도서가 활성이고 상품 재고가 장바구니 수량 이상일 때만 true입니다.
        """)
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "장바구니 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"), @ApiResponse(responseCode = "500", description = "장바구니 조회 실패")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<CartResponse>> getCart(@Parameter(hidden = true) final Long userId);
}
