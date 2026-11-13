package com.book.core.library.api.spec;

import com.book.common.api.RequiresSecurity;
import com.book.core.library.api.request.AddLibraryBooksRequest;
import com.book.core.library.api.request.CreateShelfPhotoUploadRequest;
import com.book.core.library.api.request.DeleteLibraryBooksRequest;
import com.book.core.library.api.request.ScanShelfPhotoRequest;
import com.book.core.library.api.response.LibraryBooksResponse;
import com.book.core.library.api.response.ShelfPhotoUploadResponse;
import com.book.core.library.api.response.ShelfScanResponse;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@Tag(name = "Library", description = "나의 도서관 API")
public interface LibraryControllerSpec {
    @Operation(summary = "나의 도서관 책 목록 조회", description = "인증된 회원의 삭제되지 않은 도서관 항목 전체를 최근 추가 순으로 조회합니다. 페이지네이션은 없으며, 담은 책이 없으면 빈 목록을 응답합니다.")
    @RequiresSecurity
    @ApiResponses({@ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<LibraryBooksResponse>> getBooks(@Parameter(hidden = true) final Long userId);

    @Operation(summary = "나의 도서관 책 담기",
        description = "인증된 회원이 고른 도서를 나의 도서관에 담습니다. 이미 담긴 책은 그대로 두고, 삭제했던 책은 다시 담긴 상태로 복구합니다. 존재하지 않는 도서가 하나라도 있으면 아무것도 담지 않습니다.")
    @RequiresSecurity
    @ApiResponses({@ApiResponse(responseCode = "200", description = "담기 성공"),
        @ApiResponse(responseCode = "400", description = "도서 ID 목록이 비었거나 50개를 넘거나 잘못됨"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 도서가 포함됨")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<Void>> addBooks(
        @Parameter(hidden = true) final Long userId,
        @RequestBody(required = true, content = @Content(schema = @Schema(implementation = AddLibraryBooksRequest.class)))
        @Valid final AddLibraryBooksRequest request
    );

    @Operation(summary = "나의 도서관 책 삭제",
        description = "인증된 회원이 소유한 활성 도서관 항목을 한 권 또는 여러 권 논리 삭제합니다. 존재하지 않거나 이미 삭제된 항목, 다른 회원의 항목은 무시하고 성공을 응답합니다.")
    @RequiresSecurity
    @ApiResponses({@ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "400", description = "요청 목록이 비었거나 도서관 항목 ID가 잘못됨"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<Void>> deleteBooks(
        @Parameter(hidden = true) final Long userId,
        @RequestBody(required = true, content = @Content(schema = @Schema(implementation = DeleteLibraryBooksRequest.class)))
        @Valid final DeleteLibraryBooksRequest request
    );

    @Operation(summary = "책 사진 업로드 주소 발급",
        description = "책 표지나 책장 사진을 S3에 직접 올릴 5분짜리 주소를 발급합니다. JPEG·PNG, 10MB 이하만 가능합니다. "
            + "응답의 headers를 그대로 붙이고 contentLength와 같은 크기의 파일을 PUT으로 올려야 합니다.")
    @RequiresSecurity
    @ApiResponses({@ApiResponse(responseCode = "200", description = "발급 성공"),
        @ApiResponse(responseCode = "400", description = "지원하지 않는 형식이거나 10MB를 넘음"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<ShelfPhotoUploadResponse>> createPhotoUpload(
        @Parameter(hidden = true) final Long userId,
        @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CreateShelfPhotoUploadRequest.class)))
        @Valid final CreateShelfPhotoUploadRequest request
    );

    @Operation(summary = "책 사진 인식",
        description = "본인이 올린 사진에서 책을 찾습니다. FOUND는 찾은 책, NOT_FOUND는 못 찾은 책이며, 책을 하나도 못 찾으면 items가 빈 목록입니다. "
            + "book.bookId가 null인 책은 나의 도서관에 담을 수 없습니다. 최대 65초까지 걸릴 수 있습니다.")
    @RequiresSecurity
    @ApiResponses({@ApiResponse(responseCode = "200", description = "인식 성공"),
        @ApiResponse(responseCode = "400", description = "사진 키가 비었거나 너무 김"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @ApiResponse(responseCode = "403", description = "본인이 올린 사진이 아님"),
        @ApiResponse(responseCode = "404", description = "올린 사진을 찾을 수 없음"), @ApiResponse(responseCode = "413", description = "사진이 너무 큼"),
        @ApiResponse(responseCode = "422", description = "사진을 읽을 수 없음"), @ApiResponse(responseCode = "502", description = "AI 사진 인식 실패"),
        @ApiResponse(responseCode = "503", description = "AI 사진 인식을 일시적으로 사용할 수 없음"),
        @ApiResponse(responseCode = "504", description = "AI 사진 인식 시간 초과")})
    @Parameter(in = ParameterIn.HEADER, name = HttpHeaders.AUTHORIZATION, required = true, example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<ShelfScanResponse>> scanPhoto(
        @Parameter(hidden = true) final Long userId,
        @RequestBody(required = true, content = @Content(schema = @Schema(implementation = ScanShelfPhotoRequest.class)))
        @Valid final ScanShelfPhotoRequest request
    );
}
