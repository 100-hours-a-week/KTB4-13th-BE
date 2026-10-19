package com.book.core.preference.api;

import com.book.common.config.security.UserId;
import com.book.common.response.ApiResponse;
import com.book.core.preference.api.converter.UserPreferenceCommandConverter;
import com.book.core.preference.api.converter.UserPreferenceResultConverter;
import com.book.core.preference.api.response.UserPreferencesResponse;
import com.book.core.preference.api.spec.UserPreferenceControllerSpec;
import com.book.core.preference.application.service.UserPreferenceService;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/users/me")
class UserPreferenceController implements UserPreferenceControllerSpec {
    private final UserPreferenceService userPreferenceService;
    private final UserPreferenceCommandConverter commandConverter;
    private final UserPreferenceResultConverter resultConverter;

    @Override
    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<UserPreferencesResponse>> getPreferences(@UserId final Long userId) {
        final var response = resultConverter.toUserPreferencesResponse(userPreferenceService.getPreferences(userId));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Override
    @DeleteMapping("/memories")
    public ResponseEntity<Void> deleteMemories(@UserId final Long userId,
        @NotEmpty @RequestParam("ids") final List<@NotNull @Positive Long> memoryIds) {
        userPreferenceService.deleteMemories(commandConverter.toDeleteUserMemoriesCommand(userId, memoryIds));
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/memories/all")
    public ResponseEntity<Void> deleteAllMemories(@UserId final Long userId) {
        userPreferenceService.deleteAllMemories(userId);
        return ResponseEntity.noContent().build();
    }
}
