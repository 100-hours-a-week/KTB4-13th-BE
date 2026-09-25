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
    @Operation(summary = "주문 생성", description = "활성 장바구니 상품과 회원 소유의 활성 배송지로 주문을 생성합니다. " + "재고는 검증만 하며 차감하지 않습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "주문 생성 성공"),
        @ApiResponse(responseCode = "400", description = "요청 형식이 올바르지 않거나 주문 상품이 일치하지 않음"),
        @ApiResponse(responseCode = "403", description = "배송지에 접근할 수 없음"),
        @ApiResponse(responseCode = "404", description = "활성 상품을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "알 수 없는 오류")
    })
    ResponseEntity<com.book.common.response.ApiResponse<CreateOrderResponse>> createOrder(
            @Parameter(in = ParameterIn.QUERY, required = true, example = "42") @Positive @RequestParam("userId")
                    final Long userId,
            @RequestBody(
                            description = "배송지와 주문할 장바구니 상품",
                            required = true,
                            content = @Content(schema = @Schema(implementation = CreateOrderRequest.class)))
                    @Valid
                    final CreateOrderRequest request);
}
