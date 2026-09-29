package com.book.core.search.application.port;

import java.util.List;

public record BookSearchResult(List<BookSearchItem>items,String nextCursor,String fallbackMessage,String degraded){}
