package com.book.core.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.application.usecase.GetBooksUseCase;
import com.book.core.book.domain.Book;
import com.book.core.onboarding.application.port.OnboardingBookCandidateRepositoryPort;
import com.book.core.onboarding.application.result.OnboardingBookCandidateResult;
import com.book.core.onboarding.domain.OnboardingBookCandidate;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class GetOnboardingBookCandidatesUseCaseTest {
    private final FakeCandidateRepository candidateRepository = new FakeCandidateRepository();
    private final FakeBookRepository bookRepository = new FakeBookRepository();
    private final GetOnboardingBookCandidatesUseCase useCase =
        new GetOnboardingBookCandidatesUseCase(candidateRepository, new GetBooksUseCase(bookRepository));

    @Test
    void display_order_순으로_실제_Book_정보와_함께_조회한다() {
        bookRepository.books.put(10L, book(10L, "제목10"));
        bookRepository.books.put(20L, book(20L, "제목20"));
        candidateRepository.candidates.add(new OnboardingBookCandidate(2L, 20L, 2));
        candidateRepository.candidates.add(new OnboardingBookCandidate(1L, 10L, 1));

        final List<OnboardingBookCandidateResult> results = useCase.execute();

        assertThat(results).hasSize(2);
        assertThat(results.get(0).bookId()).isEqualTo(10L);
        assertThat(results.get(0).title()).isEqualTo("제목10");
        assertThat(results.get(1).bookId()).isEqualTo(20L);
    }

    @Test
    void 후보가_없으면_빈_목록을_반환한다() {
        final List<OnboardingBookCandidateResult> results = useCase.execute();

        assertThat(results).isEmpty();
    }

    @Test
    void 존재하지_않는_Book을_참조하는_후보는_제외한다() {
        bookRepository.books.put(10L, book(10L, "제목10"));
        candidateRepository.candidates.add(new OnboardingBookCandidate(1L, 10L, 1));
        candidateRepository.candidates.add(new OnboardingBookCandidate(2L, 999L, 2));

        final List<OnboardingBookCandidateResult> results = useCase.execute();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).bookId()).isEqualTo(10L);
    }

    private Book book(final long id, final String title) {
        return new Book(id, null, null, title, "작가", null, "출판사", "소설", LocalDate.of(2026, 1, 1), "https://example.com/cover.jpg");
    }

    private static class FakeCandidateRepository implements OnboardingBookCandidateRepositoryPort {
        final List<OnboardingBookCandidate> candidates = new java.util.ArrayList<>();

        @Override
        public List<OnboardingBookCandidate> findBySubcategoryCodes(final List<String> subcategoryCodes) {
            return candidates.stream().filter(candidate -> subcategoryCodes.contains(candidate.subcategoryCode())).toList();
        }

        @Override
        public List<OnboardingBookCandidate> findAllOrderByDisplayOrder() {
            return candidates.stream().sorted(java.util.Comparator.comparingInt(OnboardingBookCandidate::displayOrder)).toList();
        }
    }

    private static class FakeBookRepository implements BookRepositoryPort {
        final Map<Long, Book> books = new HashMap<>();

        @Override
        public List<Book> findAllByIdIn(final List<Long> ids) {
            return ids.stream().map(books::get).filter(Objects::nonNull).toList();
        }
    }
}
