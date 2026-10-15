package com.book.core.library.application.command;

import java.util.List;

public record DeleteLibraryBooksCommand(Long userId,List<Long>libraryBookIds){}
