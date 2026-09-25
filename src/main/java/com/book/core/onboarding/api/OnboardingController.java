package com.book.core.onboarding.api;

import com.book.common.response.ApiResponse;
import com.book.core.onboarding.api.converter.OnboardingCommandConverter;
import com.book.core.onboarding.api.converter.OnboardingResultConverter;
import com.book.core.onboarding.api.response.OnboardingQuestionResponse;
import com.book.core.onboarding.api.spec.OnboardingControllerSpec;
import com.book.core.onboarding.application.service.OnboardingService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/onboarding")
class OnboardingController implements OnboardingControllerSpec {
    private final OnboardingService onboardingService;
    private final OnboardingCommandConverter commandConverter;
    private final OnboardingResultConverter resultConverter;

    @Override
    @GetMapping("/questions/{questionId}")
    public ResponseEntity<ApiResponse<OnboardingQuestionResponse>> getQuestion(
            @AuthenticationPrincipal final Jwt jwt, @Positive @PathVariable("questionId") final Long questionId) {
        final Long userId = Long.parseLong(jwt.getSubject());
        final var command = commandConverter.toGetOnboardingQuestionCommand(userId, questionId);
        final var response = resultConverter.toOnboardingQuestionResponse(onboardingService.getQuestion(command));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
