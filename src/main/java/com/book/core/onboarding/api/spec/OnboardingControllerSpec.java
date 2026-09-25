package com.book.core.onboarding.api.spec;

import com.book.common.response.ApiResponse;
import com.book.core.onboarding.api.response.OnboardingProgressResponse;
import com.book.core.onboarding.api.response.OnboardingQuestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Onboarding", description = "온보딩 질문/응답 API")
public interface OnboardingControllerSpec {
    @Operation(
            summary = "온보딩 질문 조회",
            description = "questionId에 해당하는 온보딩 질문과 선택지를 조회합니다. "
                    + "선행 질문(Question 3) 답변에 의존하는 질문(Question 4)은 선행 답변이 있는 대분류의 선택지만 반환합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "온보딩 질문 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 questionId"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "409",
                description = "선행 질문(Question 3)에 대한 답변이 아직 없어 조회할 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "알 수 없는 오류")
    })
    @Parameter(
            in = ParameterIn.HEADER,
            name = HttpHeaders.AUTHORIZATION,
            required = true,
            example = "Bearer {accessToken}")
    ResponseEntity<ApiResponse<OnboardingQuestionResponse>> getQuestion(
            @AuthenticationPrincipal final Jwt jwt,
            @Parameter(in = ParameterIn.PATH, required = true, example = "1") @Positive @PathVariable("questionId")
                    final Long questionId);

    @Operation(summary = "온보딩 진행 정보 조회", description = "요청 회원의 활성 온보딩 진행 상태, 질문별 응답, 선택 도서 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "온보딩 진행 정보 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보가 유효하지 않음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "진행 중인 온보딩이 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "알 수 없는 오류")
    })
    @Parameter(
            in = ParameterIn.HEADER,
            name = HttpHeaders.AUTHORIZATION,
            required = true,
            example = "Bearer {accessToken}")
    ResponseEntity<ApiResponse<OnboardingProgressResponse>> getProgress(@AuthenticationPrincipal final Jwt jwt);
}
