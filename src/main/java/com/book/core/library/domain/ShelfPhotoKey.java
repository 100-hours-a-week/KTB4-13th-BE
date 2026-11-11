package com.book.core.library.domain;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.Map;
import java.util.UUID;

/** 사진으로 책 담기에 쓰는 S3 사진 키 규칙입니다. 회원별 경로로 소유자를 구분합니다. */
public final class ShelfPhotoKey {
    public static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final String PREFIX = "shelf-photos/";
    private static final Map<String, String> EXTENSIONS = Map.of("image/jpeg", "jpg", "image/png", "png");

    private ShelfPhotoKey() {}

    public static String create(final Long userId, final String contentType, final long contentLength, final UUID id) {
        final String extension = EXTENSIONS.get(contentType);
        if (extension == null || contentLength <= 0 || contentLength > MAX_BYTES) {
            throw new CoreException(ErrorCode.INVALID_SHELF_PHOTO);
        }
        return PREFIX + userId + "/" + id + "." + extension;
    }
}
