package com.book.core.review.api.spec;

import com.book.common.response.ApiResponse;
import com.book.core.review.api.request.CreateReviewRequest;
import com.book.core.review.api.request.UpdateReviewRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Review", description = "리뷰 API")
public interface ReviewControllerSpec {
    @Operation(summary = "리뷰 작성", description = "결제 완료한 주문상품에 리뷰를 작성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "리뷰 작성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 형식 오류, 리뷰 작성 가능한 주문이 없거나 이미 작성됨"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "주문상품을 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "알 수 없는 오류")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<ApiResponse<Void>> createReview(@AuthenticationPrincipal final Jwt jwt,
        @RequestBody @Valid final CreateReviewRequest request);

    @Operation(summary = "리뷰 수정", description = "전달한 필드만 리뷰를 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "리뷰 수정 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 형식 오류"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "리뷰 작성자가 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "알 수 없는 오류")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<ApiResponse<Void>> updateReview(@AuthenticationPrincipal final Jwt jwt,
        @Positive @PathVariable("reviewId") final Long reviewId, @RequestBody @Valid final UpdateReviewRequest request);

    @Operation(summary = "리뷰 삭제", description = "리뷰를 소프트 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "리뷰 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "리뷰 작성자가 아님"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "리뷰를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "알 수 없는 오류")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<ApiResponse<Void>> deleteReview(@AuthenticationPrincipal final Jwt jwt,
        @Positive @PathVariable("reviewId") final Long reviewId);
}
