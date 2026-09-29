package com.book.core.order.api.spec;

import com.book.core.order.api.request.CreateOrderRequest;
import com.book.core.order.api.response.CreateOrderResponse;
import com.book.core.order.api.response.OrderDetailResponse;
import com.book.core.order.api.response.OrderListResponse;
import com.book.core.order.domain.OrderStatus;
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
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Order", description = "주문 API")
public interface OrderControllerSpec {
    @Operation(summary = "내 주문 목록 조회",
        description = "인증 회원의 활성 주문을 createdAt DESC, id DESC 순서로 조회합니다. 조회 시각 기본값은 오늘까지 최근 1년이며 limit은 기본 20, 최대 100입니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "주문 목록 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"), @ApiResponse(responseCode = "500", description = "알 수 없는 오류")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<OrderListResponse>> getOrders(@Parameter(hidden = true) final Long userId,
        @RequestParam(value = "status", required = false) final OrderStatus status,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam(value = "from", required = false) final LocalDateTime from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @RequestParam(value = "to", required = false) final LocalDateTime to,
        @RequestParam(value = "cursor", required = false) final String cursor,
        @RequestParam(value = "limit", required = false) final Integer limit);

    @Operation(summary = "내 주문 상세 조회", description = "주문 소유권을 확인하고 주문 상품을 포함한 상세 정보를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "주문 상세 조회 성공"),
        @ApiResponse(responseCode = "400", description = "요청 형식 오류(E400)"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @ApiResponse(responseCode = "403", description = "주문 소유자가 아님(E403)"),
        @ApiResponse(responseCode = "404", description = "주문 없음(ORDER_NOT_FOUND)"),
        @ApiResponse(responseCode = "500", description = "알 수 없는 오류")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<OrderDetailResponse>> getOrder(@Parameter(hidden = true) final Long userId,
        @Parameter(in = ParameterIn.PATH, required = true, description = "주문 키") @NotBlank @Size(max = 255)
        @PathVariable("orderKey") final String orderKey);

    @Operation(summary = "주문 생성", description = "본인의 활성 배송지와 장바구니 상품을 검증한 뒤 주문 시점 스냅샷을 생성합니다. 재고는 검증만 하며 차감하지 않습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "생성된 주문 키"),
        @ApiResponse(responseCode = "400", description = "요청 형식 오류(E400), 장바구니 상품 불일치(E3000), 재고 부족(E8001)"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @ApiResponse(responseCode = "404", description = "활성 상품을 찾을 수 없음"), @ApiResponse(responseCode = "500", description = "알 수 없는 오류")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<CreateOrderResponse>> createOrder(@Parameter(hidden = true) final Long userId,
        @RequestBody(description = "선택한 배송지와 주문할 장바구니 상품", required = true,
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
    ResponseEntity<com.book.common.response.ApiResponse<Void>> cancelOrder(@Parameter(hidden = true) final Long userId,
        @Parameter(in = ParameterIn.PATH, required = true, description = "주문 키") @NotBlank @Size(max = 255)
        @PathVariable("orderKey") final String orderKey);
}
