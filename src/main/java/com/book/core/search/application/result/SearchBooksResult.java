package com.book.core.search.application.result;

import java.util.List;

public record SearchBooksResult(List<SearchBookItemResult>items,String nextCursor,String fallbackMessage,String degraded){}
