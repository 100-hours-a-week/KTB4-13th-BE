package com.book.core.library.application.command;

import java.util.List;

public record AddLibraryBooksCommand(Long userId, List<Long> bookIds) {
}
