package com.book.core.product.api.spec;

import com.book.core.product.api.response.ProductListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Product", description = "상품 API")
public interface ProductListControllerSpec {
    @Operation(summary = "상품 목록 조회", description = "카테고리에 속한 활성 상품을 cursor 방식으로 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "상품 목록 조회 성공"),
        @ApiResponse(responseCode = "400", description = "요청이 올바르지 않음"),
        @ApiResponse(responseCode = "500", description = "알 수 없는 오류")
    })
    ResponseEntity<com.book.common.response.ApiResponse<ProductListResponse>> getProducts(
            @Parameter(in = ParameterIn.QUERY, example = "7")
                    @Positive
                    @RequestParam(value = "categoryId", required = false)
                    final Long categoryId,
            @Parameter(in = ParameterIn.QUERY, example = "createdAt") @RequestParam(value = "sort", required = false)
                    final String sort,
            @Parameter(in = ParameterIn.QUERY, example = "101") @RequestParam(value = "cursor", required = false)
                    final String cursor,
            @Parameter(in = ParameterIn.QUERY, example = "20")
                    @Positive
                    @RequestParam(value = "limit", required = false)
                    final Integer limit);
}
