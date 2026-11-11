package com.book.core.library.api.response;

import com.book.core.library.application.result.ShelfPhotoUploadResult;
import java.time.Instant;
import java.util.Map;

public record ShelfPhotoUploadResponse(String photoKey, String uploadUrl, Map<String, String> headers, Instant expiresAt) {
    public static ShelfPhotoUploadResponse from(final ShelfPhotoUploadResult result) {
        return new ShelfPhotoUploadResponse(result.photoKey(), result.uploadUrl(), result.headers(), result.expiresAt());
    }
}
