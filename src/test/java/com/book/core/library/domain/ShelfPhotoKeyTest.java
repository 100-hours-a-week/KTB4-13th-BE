package com.book.core.library.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ShelfPhotoKeyTest {
    private static final UUID ID = UUID.fromString("0b6f5d1e-6c3a-4e8a-9d2b-1f0e7c6a5b4d");

    @Test
    void JPEG와_PNG는_회원별_경로에_형식에_맞는_확장자로_키를_만든다() {
        assertThat(ShelfPhotoKey.create(42L, "image/jpeg", 1, ID)).isEqualTo("shelf-photos/42/" + ID + ".jpg");
        assertThat(ShelfPhotoKey.create(42L, "image/png", ShelfPhotoKey.MAX_BYTES, ID)).isEqualTo("shelf-photos/42/" + ID + ".png");
    }

    @ParameterizedTest
    @CsvSource({"image/heic,1", "image/gif,1", "image/jpeg,0", "image/jpeg,10485761"})
    void 지원하지_않는_형식이거나_크기가_0_또는_10MB를_넘으면_거부한다(final String contentType, final long contentLength) {
        assertThatThrownBy(() -> ShelfPhotoKey.create(42L, contentType, contentLength, ID)).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_SHELF_PHOTO));
    }

    @Test
    void 본인_경로의_UUID_사진_키만_소유한_것으로_본다() {
        assertThat(ShelfPhotoKey.isOwnedBy("shelf-photos/42/" + ID + ".jpg", 42L)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
        strings = {"shelf-photos/7/0b6f5d1e-6c3a-4e8a-9d2b-1f0e7c6a5b4d.jpg", "shelf-photos/421/0b6f5d1e-6c3a-4e8a-9d2b-1f0e7c6a5b4d.jpg",
            "shelf-photos/42/../7/0b6f5d1e-6c3a-4e8a-9d2b-1f0e7c6a5b4d.jpg", "shelf-photos/42/0b6f5d1e-6c3a-4e8a-9d2b-1f0e7c6a5b4d.gif",
            "chat-images/42/0b6f5d1e-6c3a-4e8a-9d2b-1f0e7c6a5b4d.jpg", "shelf-photos/42/photo.jpg"})
    void 다른_회원_경로나_조작한_키는_소유하지_않은_것으로_본다(final String photoKey) {
        assertThat(ShelfPhotoKey.isOwnedBy(photoKey, 42L)).isFalse();
    }
}
