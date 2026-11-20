package com.book.core.book.infrastructure.client;

import com.book.core.book.application.result.NewBookCandidate;
import java.util.List;

record NationalLibraryPage(List<NewBookCandidate> candidates, boolean hasNext) {
}
