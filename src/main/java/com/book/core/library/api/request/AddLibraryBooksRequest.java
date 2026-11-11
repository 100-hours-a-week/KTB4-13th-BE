package com.book.core.library.api.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AddLibraryBooksRequest(@NotEmpty @Size(max = 50) List<@NotNull @Positive Long> bookIds) {
}
