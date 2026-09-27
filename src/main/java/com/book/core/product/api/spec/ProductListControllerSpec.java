package com.book.core.product.api.spec;

import com.book.core.product.api.response.ProductListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Product", description = "상품 API")
public interface ProductListControllerSpec {
    @Operation(summary = "상품 목록 조회",
        description = "활성 상품을 cursor 방식으로 조회합니다. sort=POPULARITY는 전체 기간 결제된 주문 상품 수량 합계 DESC, "
            + "활성 리뷰 수 DESC, 평균 평점 DESC, 상품 ID DESC 순으로 정렬합니다. 인기 지표가 없으면 0이며 재고는 반영하지 않습니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "상품 목록 조회 성공"),
        @ApiResponse(responseCode = "400", description = "요청이 올바르지 않음"), @ApiResponse(responseCode = "500", description = "알 수 없는 오류")})
    ResponseEntity<com.book.common.response.ApiResponse<ProductListResponse>> getProducts(
        @Parameter(in = ParameterIn.QUERY, example = "7") @Positive
        @RequestParam(value = "categoryId", required = false) final Long categoryId,
        @Parameter(in = ParameterIn.QUERY, description = "createdAt 또는 POPULARITY. 생략하면 createdAt입니다.", example = "POPULARITY",
            schema = @Schema(allowableValues = {"createdAt", "POPULARITY"}, defaultValue = "createdAt"))
        @RequestParam(value = "sort", required = false) final String sort,
        @Parameter(in = ParameterIn.QUERY, description = "createdAt 정렬은 상품 ID 문자열, POPULARITY 정렬은 응답의 nextCursor 값을 전달합니다.")
        @RequestParam(value = "cursor", required = false) final String cursor,
        @Parameter(in = ParameterIn.QUERY, example = "20") @Positive @RequestParam(value = "limit", required = false) final Integer limit);
}
