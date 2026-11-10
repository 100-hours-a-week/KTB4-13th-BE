package com.book.core.library.application.port;

import com.book.core.library.application.result.ShelfPhotoUploadResult;

public interface ShelfPhotoStorageClient {
    ShelfPhotoUploadResult presignUpload(String photoKey, String contentType, long contentLength);
}
