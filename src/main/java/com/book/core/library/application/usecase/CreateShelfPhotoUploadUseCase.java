package com.book.core.library.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.library.application.command.CreateShelfPhotoUploadCommand;
import com.book.core.library.application.port.ShelfPhotoStorageClient;
import com.book.core.library.application.result.ShelfPhotoUploadResult;
import com.book.core.library.domain.ShelfPhotoKey;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateShelfPhotoUploadUseCase {
    private final ShelfPhotoStorageClient storage;

    public ShelfPhotoUploadResult execute(final CreateShelfPhotoUploadCommand command) {
        final String photoKey = ShelfPhotoKey.create(command.userId(), command.contentType(), command.contentLength(), UUID.randomUUID());
        return storage.presignUpload(photoKey, command.contentType(), command.contentLength());
    }
}
