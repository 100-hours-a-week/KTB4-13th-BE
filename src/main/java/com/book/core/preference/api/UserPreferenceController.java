package com.book.core.preference.api;

import com.book.common.config.security.UserId;
import com.book.common.response.ApiResponse;
import com.book.core.preference.api.converter.UserPreferenceResultConverter;
import com.book.core.preference.api.response.UserPreferencesResponse;
import com.book.core.preference.api.spec.UserPreferenceControllerSpec;
import com.book.core.preference.application.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/preferences")
class UserPreferenceController implements UserPreferenceControllerSpec {
    private final UserPreferenceService userPreferenceService;
    private final UserPreferenceResultConverter resultConverter;

    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> getPreferences(@UserId final Long userId) {
        final var response = resultConverter.toUserPreferencesResponse(userPreferenceService.getPreferences(userId));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
