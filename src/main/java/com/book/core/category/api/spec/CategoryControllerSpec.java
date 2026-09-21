package com.book.core.category.api.spec;

import com.book.core.category.api.response.CategoryListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Category", description = "상품 카테고리 API")
public interface CategoryControllerSpec {
    @Operation(summary = "상품 카테고리 목록 조회", description = "상품 카테고리의 목록을 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "상품 카테고리 목록 조회 성공"),
        @ApiResponse(responseCode = "400", description = "요청이 올바르지 않음"),
        @ApiResponse(responseCode = "500", description = "알 수 없는 오류")
    })
    ResponseEntity<com.book.common.response.ApiResponse<CategoryListResponse>> getCategories();
}
