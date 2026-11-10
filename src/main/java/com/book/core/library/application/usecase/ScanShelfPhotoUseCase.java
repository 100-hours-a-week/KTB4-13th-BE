package com.book.core.library.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.library.application.command.ScanShelfPhotoCommand;
import com.book.core.library.application.port.ShelfPhotoStorageClient;
import com.book.core.library.application.port.ShelfScanClient;
import com.book.core.library.application.result.ShelfScanResult;
import com.book.core.library.domain.ShelfPhotoKey;
import lombok.RequiredArgsConstructor;

/** AI 응답을 최대 65초까지 기다리므로 트랜잭션 없이 실행해 DB 연결을 잡지 않습니다. */
@UseCase
@RequiredArgsConstructor
public class ScanShelfPhotoUseCase {
    private final ShelfPhotoStorageClient storage;
    private final ShelfScanClient scanClient;

    public ShelfScanResult execute(final ScanShelfPhotoCommand command) {
        if (!ShelfPhotoKey.isOwnedBy(command.photoKey(), command.userId())) {
            throw new CoreException(ErrorCode.SHELF_PHOTO_FORBIDDEN);
        }
        return scanClient.scan(command.userId(), storage.presignRead(command.photoKey()));
    }
}
