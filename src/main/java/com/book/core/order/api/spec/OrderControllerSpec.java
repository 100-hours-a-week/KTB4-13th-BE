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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Order", description = "주문 API")
public interface OrderControllerSpec {
    @Operation(summary = "주문 생성",
        description = "활성 장바구니 상품으로 주문을 생성합니다. 기본 활성 배송지가 없으면 주문은 생성하고 " + "NO_ADDRESS 상태로 반환해 결제 진행을 막습니다. 재고는 검증만 하며 차감하지 않습니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "주문 키, 상태, 총액과 상품별 주문 항목"),
        @ApiResponse(responseCode = "400", description = "요청 형식 오류(E400), 장바구니 상품 불일치(E3000), 재고 부족(E8001)"),
        @ApiResponse(responseCode = "404", description = "활성 상품을 찾을 수 없음"), @ApiResponse(responseCode = "500", description = "알 수 없는 오류")})
    ResponseEntity<com.book.common.response.ApiResponse<CreateOrderResponse>> createOrder(
        @Parameter(in = ParameterIn.QUERY, required = true, example = "42") @Positive @RequestParam("userId") final Long userId,
        @RequestBody(description = "주문할 장바구니 상품. 기본 활성 배송지는 서버가 선택합니다.", required = true,
            content = @Content(schema = @Schema(implementation = CreateOrderRequest.class)))
        @Valid final CreateOrderRequest request);
}
