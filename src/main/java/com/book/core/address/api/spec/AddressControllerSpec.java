package com.book.core.address.api.spec;

import com.book.core.address.api.request.RegisterAddressRequest;
import com.book.core.address.api.request.UpdateAddressRequest;
import com.book.core.address.api.response.AddressListResponse;
import com.book.core.address.api.response.UpdateAddressResponse;
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
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Address", description = "배송지 API")
public interface AddressControllerSpec {
    @Operation(summary = "배송지 등록", description = "요청 회원의 활성 배송지를 등록합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "배송지 등록 성공"),
        @ApiResponse(responseCode = "400", description = "요청 형식이 올바르지 않거나 등록 정책을 위반함"),
        @ApiResponse(responseCode = "401", description = "인증 연동 시 인증 정보가 유효하지 않음"),
        @ApiResponse(responseCode = "500", description = "알 수 없는 오류")
    })
    ResponseEntity<com.book.common.response.ApiResponse<Void>> registerAddress(
            @Parameter(in = ParameterIn.QUERY, required = true, example = "42") @Positive @RequestParam("userId")
                    final Long userId,
            @RequestBody(
                            description = "등록할 배송지 정보",
                            required = true,
                            content = @Content(schema = @Schema(implementation = RegisterAddressRequest.class)))
                    @Valid
                    final RegisterAddressRequest request);

    @Operation(summary = "배송지 목록 조회", description = "userId에 해당하는 활성 배송지 목록을 기본 배송지 우선으로 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "배송지 목록 조회 성공"),
        @ApiResponse(responseCode = "400", description = "요청 형식이 올바르지 않음"),
        @ApiResponse(responseCode = "500", description = "알 수 없는 오류")
    })
    ResponseEntity<com.book.common.response.ApiResponse<AddressListResponse>> getAddresses(
            @Parameter(in = ParameterIn.QUERY, required = true, example = "42") @Positive @RequestParam("userId")
                    final Long userId);

    @Operation(summary = "배송지 수정", description = "배송지 전체 정보를 교체합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "배송지 수정 성공"),
        @ApiResponse(responseCode = "400", description = "요청 형식 또는 수정 정책이 올바르지 않음"),
        @ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @ApiResponse(responseCode = "403", description = "수정 대상 배송지에 접근할 수 없음"),
        @ApiResponse(responseCode = "500", description = "알 수 없는 오류")
    })
    @Parameter(
            in = ParameterIn.HEADER,
            name = HttpHeaders.AUTHORIZATION,
            required = true,
            example = "Bearer {accessToken}")
    ResponseEntity<com.book.common.response.ApiResponse<UpdateAddressResponse>> updateAddress(
            @Parameter(in = ParameterIn.QUERY, required = true, example = "42") @Positive @RequestParam("userId")
                    final Long userId,
            @Parameter(in = ParameterIn.PATH, required = true, example = "101") @Positive @PathVariable("addressId")
                    final Long addressId,
            @RequestBody(
                            description = "교체할 배송지 전체 정보",
                            required = true,
                            content = @Content(schema = @Schema(implementation = UpdateAddressRequest.class)))
                    @Valid
                    final UpdateAddressRequest request);

}
