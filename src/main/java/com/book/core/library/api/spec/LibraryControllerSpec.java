package com.book.core.library.api.spec;

import com.book.core.library.api.request.DeleteLibraryBooksRequest;
import com.book.core.library.api.response.LibraryBooksResponse;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@Tag(name = "Library", description = "나의 도서관 API")
public interface LibraryControllerSpec {
    @Operation(summary = "나의 도서관 책 목록 조회", description = "인증된 회원의 삭제되지 않은 도서관 항목 전체를 최근 추가 순으로 조회합니다. 페이지네이션은 없으며, 담은 책이 없으면 빈 목록을 응답합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<LibraryBooksResponse>> getBooks(@Parameter(hidden = true) final Long userId);

    @Operation(summary = "나의 도서관 책 삭제",
        description = "인증된 회원이 소유한 활성 도서관 항목을 한 권 또는 여러 권 논리 삭제합니다. 존재하지 않거나 이미 삭제된 항목, 다른 회원의 항목은 무시하고 성공을 응답합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "요청 목록이 비었거나 도서관 항목 ID가 잘못됨"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<Void>> deleteBooks(@Parameter(hidden = true) final Long userId,
        @RequestBody(required = true, content = @Content(schema = @Schema(implementation = DeleteLibraryBooksRequest.class)))
        @Valid final DeleteLibraryBooksRequest request);
}
