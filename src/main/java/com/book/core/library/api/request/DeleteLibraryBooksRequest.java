package com.book.core.library.api.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record DeleteLibraryBooksRequest(@NotEmpty List<@NotNull @Positive Long>libraryBookIds){}
