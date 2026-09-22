package com.book.core.auth.api.spec;

import com.book.core.auth.api.request.AuthLoginRequest;
import com.book.core.auth.api.response.AuthLoginResponse;
import com.book.core.auth.api.response.AuthReissueResponse;
import com.book.core.user.domain.ProviderType;
import com.book.support.web.SuccessResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

@Tag(name = "Auth", description = "인증 API")
public interface AuthControllerSpec {

    @Operation(summary = "소셜 로그인", description = "외부 provider 인가 코드로 로그인하고 서비스 Token을 발급합니다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "로그인 성공",
                content = @Content(schema = @Schema(implementation = AuthLoginResponse.class))),
        @ApiResponse(responseCode = "400", description = "지원하지 않는 provider 또는 잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 인가 코드 또는 provider ID Token"),
        @ApiResponse(responseCode = "502", description = "외부 인증 서비스 연동 실패")
    })
    ResponseEntity<SuccessResponse<AuthLoginResponse>> login(
            @Parameter(description = "소셜 로그인 provider", required = true, example = "kakao")
                    final ProviderType providerType,
            @RequestBody(
                            description = "소셜 로그인 인가 코드, PKCE verifier, nonce",
                            required = true,
                            content = @Content(schema = @Schema(implementation = AuthLoginRequest.class)))
                    final AuthLoginRequest request);

    @Operation(summary = "토큰 재발급", description = "Refresh Token을 검증하고 새로운 Access/Refresh Token을 발급합니다.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "토큰 재발급 성공",
                content = @Content(schema = @Schema(implementation = AuthReissueResponse.class))),
        @ApiResponse(responseCode = "401", description = "유효하지 않거나 만료된 Refresh Token")
    })

    ResponseEntity<SuccessResponse<AuthReissueResponse>> reissue(final HttpServletRequest request);

    @Operation(
        summary = "로그아웃",
        description = "현재 사용자의 Refresh Session을 폐기하고 Refresh Token Cookie를 만료합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 Access Token")
    })
    ResponseEntity<Void> logout(final Jwt jwt);
}
