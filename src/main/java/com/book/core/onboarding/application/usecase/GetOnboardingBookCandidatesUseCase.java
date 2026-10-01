package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.book.application.usecase.GetBooksUseCase;
import com.book.core.book.domain.Book;
import com.book.core.onboarding.application.command.GetOnboardingBookCandidatesCommand;
import com.book.core.onboarding.application.port.OnboardingBookCandidateRepositoryPort;
import com.book.core.onboarding.application.port.OnboardingOptionRepositoryPort;
import com.book.core.onboarding.application.result.OnboardingBookCandidateResult;
import com.book.core.onboarding.domain.OnboardingBookCandidate;
import com.book.core.onboarding.domain.OnboardingOption;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class GetOnboardingBookCandidatesUseCase {
    private final OnboardingBookCandidateRepositoryPort candidateRepository;
    private final GetBooksUseCase getBooksUseCase;
    private final OnboardingOptionRepositoryPort optionRepository;

    @Transactional(readOnly = true)
    public List<OnboardingBookCandidateResult> execute(final GetOnboardingBookCandidatesCommand command) {
        // TODO: 세부 카테고리 단계의 질문 ID(4L)를 온보딩 단계를 나타내는 Enum 값으로 대체한다. 작성자: 023-dev
        final Set<String> validCodes =
            optionRepository.findByQuestionId(4L).stream().map(OnboardingOption::code).collect(Collectors.toSet());
        if (!validCodes.containsAll(command.subcategoryCodes())) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_SUBCATEGORY_SELECTION);
        }
        final List<OnboardingBookCandidate> candidates = candidateRepository.findBySubcategoryCodes(command.subcategoryCodes()).stream()
            .sorted(Comparator
                .comparingInt((final OnboardingBookCandidate candidate) -> command.subcategoryCodes().indexOf(candidate.subcategoryCode()))
                .thenComparingInt(OnboardingBookCandidate::displayOrder).thenComparing(OnboardingBookCandidate::bookId))
            .toList();
        final List<Long> bookIds = candidates.stream().map(OnboardingBookCandidate::bookId).distinct().toList();
        if (bookIds.isEmpty()) {
            return List.of();
        }
        final Map<Long, Book> booksById =
            getBooksUseCase.execute(bookIds).stream().collect(Collectors.toMap(Book::id, Function.identity()));

        return bookIds.stream().map(booksById::get).filter(Objects::nonNull).map(this::toResult).toList();
    }

    private OnboardingBookCandidateResult toResult(final Book book) {
        return new OnboardingBookCandidateResult(book.id(), book.title(), book.author(), book.coverImageUrl());
    }
}
