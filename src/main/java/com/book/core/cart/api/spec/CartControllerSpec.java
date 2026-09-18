package com.book.core.cart.api.spec;

import com.book.core.cart.api.request.CartAddRequest;
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

@Tag(name = "Cart", description = "장바구니 API")
public interface CartControllerSpec {
    @Operation(
            summary = "장바구니 상품 추가",
            description = "동일 장바구니에 같은 상품이 있으면 기존 수량에 누적하지 않고 요청한 수량으로 대체합니다. "
                    + "같은 추가 요청을 반복해도 결과는 같습니다. quantity를 생략하면 1개를 추가합니다.")
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
}
