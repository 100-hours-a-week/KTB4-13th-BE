package com.book.core.library.api;

import com.book.common.config.security.UserId;
import com.book.common.response.ApiResponse;
import com.book.core.library.api.converter.LibraryCommandConverter;
import com.book.core.library.api.request.DeleteLibraryBooksRequest;
import com.book.core.library.api.response.LibraryBooksResponse;
import com.book.core.library.api.spec.LibraryControllerSpec;
import com.book.core.library.application.service.LibraryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/library")
class LibraryController implements LibraryControllerSpec {
    private final LibraryService libraryService;
    private final LibraryCommandConverter commandConverter;

    @Override
    @GetMapping("/books")
    public ResponseEntity<ApiResponse<LibraryBooksResponse>> getBooks(@UserId final Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(LibraryBooksResponse.from(libraryService.getBooks(userId))));
    }

    @Override
    @DeleteMapping("/books")
    public ResponseEntity<ApiResponse<Void>> deleteBooks(@UserId final Long userId,
        @Valid @RequestBody final DeleteLibraryBooksRequest request) {
        libraryService.deleteBooks(commandConverter.toDeleteLibraryBooksCommand(userId, request));
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
