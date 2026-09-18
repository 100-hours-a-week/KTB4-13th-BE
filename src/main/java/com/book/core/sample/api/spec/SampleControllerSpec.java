package com.book.core.sample.api.spec;

import com.book.core.sample.api.request.SampleCreateRequest;
import com.book.core.sample.api.response.SampleCreateResponse;
import com.book.core.sample.api.response.SampleQueryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Sample", description = "샘플 API")
public interface SampleControllerSpec {

    @Operation(summary = "샘플 생성", description = "샘플 이름을 등록하고 생성된 샘플 정보를 반환합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "샘플 생성 성공"),
        @ApiResponse(responseCode = "400", description = "요청 형식이 올바르지 않거나 이름 규칙을 위반함")
    })
    ResponseEntity<com.book.common.response.ApiResponse<SampleCreateResponse>> create(
            @RequestBody(
                            description = "생성할 샘플 정보",
                            required = true,
                            content = @Content(schema = @Schema(implementation = SampleCreateRequest.class)))
                    final SampleCreateRequest request);

    @Operation(summary = "샘플 단건 조회", description = "샘플 ID로 샘플 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "샘플 조회 성공"),
        @ApiResponse(responseCode = "400", description = "샘플 ID가 양수가 아님"),
        @ApiResponse(responseCode = "404", description = "샘플을 찾을 수 없음")
    })
    com.book.common.response.ApiResponse<SampleQueryResponse> query(
            @Parameter(description = "조회할 샘플 ID", required = true, example = "1") final Long sampleId);
}
