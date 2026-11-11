package com.book.core.library.application.result;

import java.time.Instant;
import java.util.Map;

public record ShelfPhotoUploadResult(String photoKey, String uploadUrl, Map<String, String> headers, Instant expiresAt) {
}
