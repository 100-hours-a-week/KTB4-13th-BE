package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.book.application.usecase.GetBooksUseCase;
import com.book.core.book.domain.Book;
import com.book.core.onboarding.application.port.OnboardingBookCandidateRepositoryPort;
import com.book.core.onboarding.application.result.OnboardingBookCandidateResult;
import com.book.core.onboarding.domain.OnboardingBookCandidate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetOnboardingBookCandidatesUseCase {
    private final OnboardingBookCandidateRepositoryPort candidateRepository;
    private final GetBooksUseCase getBooksUseCase;

    @Transactional(readOnly = true)
    public List<OnboardingBookCandidateResult> execute() {
        final List<OnboardingBookCandidate> candidates = candidateRepository.findAllOrderByDisplayOrder();
        final List<Long> bookIds = candidates.stream().map(OnboardingBookCandidate::bookId).toList();
        final Map<Long, Book> booksById =
            getBooksUseCase.execute(bookIds).stream().collect(Collectors.toMap(Book::id, Function.identity()));

        return candidates.stream().filter(candidate -> booksById.containsKey(candidate.bookId()))
            .map(candidate -> toResult(booksById.get(candidate.bookId()))).toList();
    }

    private OnboardingBookCandidateResult toResult(final Book book) {
        return new OnboardingBookCandidateResult(book.id(), book.title(), book.author(), book.coverImageUrl());
    }
}
