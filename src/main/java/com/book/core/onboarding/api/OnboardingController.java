package com.book.core.onboarding.api;

import com.book.common.response.ApiResponse;
import com.book.core.onboarding.api.converter.OnboardingCommandConverter;
import com.book.core.onboarding.api.converter.OnboardingResultConverter;
import com.book.core.onboarding.api.request.PutOnboardingAnswersRequest;
import com.book.core.onboarding.api.request.PutOnboardingBooksRequest;
import com.book.core.onboarding.api.response.OnboardingBookCandidatesResponse;
import com.book.core.onboarding.api.response.OnboardingProgressResponse;
import com.book.core.onboarding.api.response.OnboardingQuestionResponse;
import com.book.core.onboarding.api.response.PersonalizedRecommendationConsentResponse;
import com.book.core.onboarding.api.spec.OnboardingControllerSpec;
import com.book.core.onboarding.application.service.OnboardingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    public ResponseEntity<ApiResponse<OnboardingQuestionResponse>> getQuestion(@AuthenticationPrincipal final Jwt jwt,
        @Positive @PathVariable("questionId") final Long questionId) {
        final Long userId = Long.parseLong(jwt.getSubject());
        final var command = commandConverter.toGetOnboardingQuestionCommand(userId, questionId);
        final var response = resultConverter.toOnboardingQuestionResponse(onboardingService.getQuestion(command));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<OnboardingProgressResponse>> getProgress(@AuthenticationPrincipal final Jwt jwt) {
        final Long userId = Long.parseLong(jwt.getSubject());
        final var command = commandConverter.toGetOnboardingProgressCommand(userId);
        final var response = resultConverter.toOnboardingProgressResponse(onboardingService.getProgress(command));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @PutMapping("/questions/{questionId}/answers")
    public ResponseEntity<ApiResponse<Void>> saveAnswers(@AuthenticationPrincipal final Jwt jwt,
        @Positive @PathVariable("questionId") final Long questionId, @Valid @RequestBody final PutOnboardingAnswersRequest request) {
        final Long userId = Long.parseLong(jwt.getSubject());
        final var command = commandConverter.toPutOnboardingAnswersCommand(userId, questionId, request);
        onboardingService.saveAnswers(command);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    @GetMapping("/books")
    public ResponseEntity<ApiResponse<OnboardingBookCandidatesResponse>> getBookCandidates(@AuthenticationPrincipal final Jwt jwt) {
        final var response = resultConverter.toOnboardingBookCandidatesResponse(onboardingService.getBookCandidates());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @PutMapping("/books")
    public ResponseEntity<ApiResponse<Void>> saveBooks(@AuthenticationPrincipal final Jwt jwt,
        @Valid @RequestBody final PutOnboardingBooksRequest request) {
        final Long userId = Long.parseLong(jwt.getSubject());
        final var command = commandConverter.toPutOnboardingBooksCommand(userId, request);
        onboardingService.saveBooks(command);
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Override
    @PostMapping("/consent")
    public ResponseEntity<ApiResponse<PersonalizedRecommendationConsentResponse>> recordConsent(@AuthenticationPrincipal final Jwt jwt) {
        final Long userId = Long.parseLong(jwt.getSubject());
        final var command = commandConverter.toRecordPersonalizedRecommendationConsentCommand(userId);
        final var response = resultConverter.toPersonalizedRecommendationConsentResponse(onboardingService.recordConsent(command));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @GetMapping("/consent")
    public ResponseEntity<ApiResponse<PersonalizedRecommendationConsentResponse>> getConsent(@AuthenticationPrincipal final Jwt jwt) {
        final Long userId = Long.parseLong(jwt.getSubject());
        final var command = commandConverter.toGetPersonalizedRecommendationConsentCommand(userId);
        final var response = resultConverter.toPersonalizedRecommendationConsentResponse(onboardingService.getConsent(command));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
