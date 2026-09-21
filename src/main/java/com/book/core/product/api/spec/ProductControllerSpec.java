package com.book.core.product.api.spec;

import com.book.core.product.api.response.ProductDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Product", description = "상품 API")
public interface ProductControllerSpec {
    @Operation(summary = "상품 상세 조회", description = "상품 식별자로 활성 상품의 상세 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "상품 상세 조회 성공"),
        @ApiResponse(responseCode = "400", description = "요청이 올바르지 않음"),
        @ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
        @ApiResponse(responseCode = "500", description = "알 수 없는 오류")
    })
    ResponseEntity<com.book.common.response.ApiResponse<ProductDetailResponse>> getProductDetail(
            @Parameter(in = ParameterIn.PATH, required = true, example = "1") @Positive @PathVariable("productId")
                    final Long productId);
}
