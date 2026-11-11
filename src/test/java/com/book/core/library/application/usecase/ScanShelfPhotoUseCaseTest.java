package com.book.core.library.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.library.application.command.ScanShelfPhotoCommand;
import com.book.core.library.application.port.ShelfPhotoStorageClient;
import com.book.core.library.application.port.ShelfScanClient;
import com.book.core.library.application.result.ShelfScanResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScanShelfPhotoUseCaseTest {
    private static final String OWN_KEY = "shelf-photos/42/0b6f5d1e-6c3a-4e8a-9d2b-1f0e7c6a5b4d.png";

    private final ShelfPhotoStorageClient storage = mock(ShelfPhotoStorageClient.class);
    private final ShelfScanClient scanClient = mock(ShelfScanClient.class);
    private final ScanShelfPhotoUseCase scan = new ScanShelfPhotoUseCase(storage, scanClient);

    @Test
    void 본인_사진은_읽기_주소를_서명해_AI에_전달한다() {
        final ShelfScanResult expected = new ShelfScanResult("cover", List.of());
        when(storage.presignRead(OWN_KEY)).thenReturn("https://s3/read");
        when(scanClient.scan(42L, "https://s3/read")).thenReturn(expected);

        assertThat(scan.execute(new ScanShelfPhotoCommand(42L, OWN_KEY))).isEqualTo(expected);
    }

    @Test
    void 다른_회원의_사진은_읽기_주소를_만들거나_AI를_부르지_않고_403으로_거부한다() {
        assertThatThrownBy(() -> scan.execute(new ScanShelfPhotoCommand(7L, OWN_KEY))).isInstanceOfSatisfying(CoreException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.SHELF_PHOTO_FORBIDDEN));
        verifyNoInteractions(storage, scanClient);
    }
}
