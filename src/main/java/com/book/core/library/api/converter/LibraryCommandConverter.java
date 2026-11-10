package com.book.core.library.api.converter;

import com.book.core.library.api.request.CreateShelfPhotoUploadRequest;
import com.book.core.library.api.request.DeleteLibraryBooksRequest;
import com.book.core.library.application.command.CreateShelfPhotoUploadCommand;
import com.book.core.library.application.command.DeleteLibraryBooksCommand;
import org.springframework.stereotype.Component;

@Component
public class LibraryCommandConverter {
    public DeleteLibraryBooksCommand toDeleteLibraryBooksCommand(final Long userId, final DeleteLibraryBooksRequest request) {
        return new DeleteLibraryBooksCommand(userId, request.libraryBookIds());
    }

    public CreateShelfPhotoUploadCommand toCreateShelfPhotoUploadCommand(final Long userId, final CreateShelfPhotoUploadRequest request) {
        return new CreateShelfPhotoUploadCommand(userId, request.contentType(), request.contentLength());
    }
}
