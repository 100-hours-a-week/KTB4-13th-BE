package com.book.core.library.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.library.application.command.CreateShelfPhotoUploadCommand;
import com.book.core.library.application.port.ShelfPhotoStorageClient;
import com.book.core.library.application.result.ShelfPhotoUploadResult;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CreateShelfPhotoUploadUseCaseTest {
    private final ShelfPhotoStorageClient storage = mock(ShelfPhotoStorageClient.class);
    private final CreateShelfPhotoUploadUseCase createUpload = new CreateShelfPhotoUploadUseCase(storage);

    @Test
    void 업로드_주소는_회원_경로의_새_키로_형식과_크기를_서명해_발급한다() {
        when(storage.presignUpload(matches("shelf-photos/42/[0-9a-f-]{36}\\.png"), eq("image/png"), eq(2048L)))
            .thenAnswer(invocation -> new ShelfPhotoUploadResult(invocation.getArgument(0), "https://s3/upload",
                Map.of("content-type", "image/png"), Instant.EPOCH));

        final ShelfPhotoUploadResult result = createUpload.execute(new CreateShelfPhotoUploadCommand(42L, "image/png", 2048L));

        assertThat(result.photoKey()).matches("shelf-photos/42/[0-9a-f-]{36}\\.png");
        assertThat(result).extracting(ShelfPhotoUploadResult::uploadUrl, ShelfPhotoUploadResult::headers, ShelfPhotoUploadResult::expiresAt)
            .containsExactly("https://s3/upload", Map.of("content-type", "image/png"), Instant.EPOCH);
    }

    @Test
    void 지원하지_않는_사진은_업로드_주소를_발급하지_않는다() {
        assertThatThrownBy(() -> createUpload.execute(new CreateShelfPhotoUploadCommand(42L, "image/heic", 2048L))).isInstanceOfSatisfying(
            CoreException.class, exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_SHELF_PHOTO));
        verifyNoInteractions(storage);
    }
}
