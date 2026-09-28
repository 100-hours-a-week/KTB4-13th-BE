package com.book.core.order.api.spec;

import com.book.core.order.api.request.CreateOrderRequest;
import com.book.core.order.api.response.CreateOrderResponse;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Order", description = "주문 API")
public interface OrderControllerSpec {
    @Operation(summary = "주문 생성",
        description = "활성 장바구니 상품으로 주문을 생성합니다. 기본 활성 배송지가 없으면 주문은 생성하고 " + "NO_ADDRESS 상태로 반환해 결제 진행을 막습니다. 재고는 검증만 하며 차감하지 않습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "주문 키, 상태, 총액과 상품별 주문 항목"),
        @ApiResponse(responseCode = "400", description = "요청 형식 오류(E400), 장바구니 상품 불일치(E3000), 재고 부족(E8001)"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @ApiResponse(responseCode = "404", description = "활성 상품을 찾을 수 없음"), @ApiResponse(responseCode = "500", description = "알 수 없는 오류")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<CreateOrderResponse>> createOrder(@AuthenticationPrincipal final Jwt jwt,
        @RequestBody(description = "주문할 장바구니 상품. 기본 활성 배송지는 서버가 선택합니다.", required = true,
            content = @Content(schema = @Schema(implementation = CreateOrderRequest.class)))
        @Valid final CreateOrderRequest request);

    @Operation(summary = "주문 전체 취소", description = "주문 소유자가 결제 전 생성한 주문 전체를 취소합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "주문과 주문상품 취소 완료"),
        @ApiResponse(responseCode = "400", description = "요청 형식 오류(E400)"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @ApiResponse(responseCode = "403", description = "주문 소유자가 아님(E403)"),
        @ApiResponse(responseCode = "404", description = "주문 없음(ORDER_NOT_FOUND)"),
        @ApiResponse(responseCode = "409", description = "취소할 수 없는 주문 상태(ORDER_CANNOT_BE_CANCELED)"),
        @ApiResponse(responseCode = "500", description = "알 수 없는 오류")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<Void>> cancelOrder(@AuthenticationPrincipal final Jwt jwt,
        @Parameter(in = ParameterIn.PATH, required = true, description = "주문 키") @NotBlank @Size(max = 255)
        @PathVariable("orderKey") final String orderKey);
}
