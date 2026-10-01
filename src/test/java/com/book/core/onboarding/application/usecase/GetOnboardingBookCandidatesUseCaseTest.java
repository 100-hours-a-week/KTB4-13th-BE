package com.book.core.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import java.time.LocalDate;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetOnboardingBookCandidatesUseCaseTest {
    private final OnboardingBookCandidateRepositoryPort candidateRepository = mock(OnboardingBookCandidateRepositoryPort.class);
    private final GetBooksUseCase getBooksUseCase = mock(GetBooksUseCase.class);
    private final OnboardingOptionRepositoryPort optionRepository = mock(OnboardingOptionRepositoryPort.class);
    private final GetOnboardingBookCandidatesUseCase useCase =
        new GetOnboardingBookCandidatesUseCase(candidateRepository, getBooksUseCase, optionRepository);

    @BeforeEach
    void subcategories() {
        when(optionRepository.findByQuestionId(4L)).thenReturn(
            List.of(new OnboardingOption(24L, 4L, 9L, "novel-sf", "SF", 2), new OnboardingOption(51L, 4L, 16L, "science-space", "우주", 3)));
    }

    @Test
    void 요청_카테고리와_rank_bookId_순으로_정렬하고_중복_도서를_제거한다() {
        final var command = new GetOnboardingBookCandidatesCommand(List.of("science-space", "novel-sf"));
        when(candidateRepository.findBySubcategoryCodes(command.subcategoryCodes())).thenReturn(
            List.of(new OnboardingBookCandidate(1L, 10L, "novel-sf", 1), new OnboardingBookCandidate(2L, 20L, "science-space", 1),
                new OnboardingBookCandidate(3L, 10L, "science-space", 1), new OnboardingBookCandidate(4L, 30L, "novel-sf", 2)));
        when(getBooksUseCase.execute(List.of(10L, 20L, 30L))).thenReturn(List.of(book(30L), book(20L), book(10L)));

        final List<OnboardingBookCandidateResult> results = useCase.execute(command);

        assertThat(results).extracting(OnboardingBookCandidateResult::bookId).containsExactly(10L, 20L, 30L);
        assertThat(results.getFirst()).usingRecursiveComparison().isEqualTo(new OnboardingBookCandidateResult(10L, "제목10", "작가", null));
        verify(candidateRepository).findBySubcategoryCodes(List.of("science-space", "novel-sf"));
        verify(getBooksUseCase).execute(List.of(10L, 20L, 30L));
    }

    @Test
    void 후보_20권을_15권으로_제한하지_않고_전체_반환한다() {
        final var command = new GetOnboardingBookCandidatesCommand(List.of("novel-sf", "science-space"));
        when(candidateRepository.findBySubcategoryCodes(command.subcategoryCodes()))
            .thenReturn(LongStream.rangeClosed(1, 20).mapToObj((final long id) -> {
                if (id <= 10) {
                    return new OnboardingBookCandidate(id, id, "novel-sf", (int) id);
                }
                return new OnboardingBookCandidate(id, id, "science-space", (int) id - 10);
            }).toList());
        when(getBooksUseCase.execute(anyList())).thenReturn(LongStream.rangeClosed(1, 20).mapToObj(this::book).toList());

        assertThat(useCase.execute(command)).extracting(OnboardingBookCandidateResult::bookId)
            .containsExactlyElementsOf(LongStream.rangeClosed(1, 20).boxed().toList());
    }

    @Test
    void 유효한_카테고리에_후보가_없으면_도서_조회_없이_빈_목록을_반환한다() {
        final var command = new GetOnboardingBookCandidatesCommand(List.of("novel-sf"));
        when(candidateRepository.findBySubcategoryCodes(command.subcategoryCodes())).thenReturn(List.of());

        assertThat(useCase.execute(command)).isEmpty();
        verifyNoInteractions(getBooksUseCase);
    }

    @Test
    void 존재하지_않는_도서는_제외한다() {
        final var command = new GetOnboardingBookCandidatesCommand(List.of("novel-sf"));
        when(candidateRepository.findBySubcategoryCodes(command.subcategoryCodes()))
            .thenReturn(List.of(new OnboardingBookCandidate(1L, 10L, "novel-sf", 1), new OnboardingBookCandidate(2L, 999L, "novel-sf", 2)));
        when(getBooksUseCase.execute(List.of(10L, 999L))).thenReturn(List.of(book(10L)));

        assertThat(useCase.execute(command)).extracting(OnboardingBookCandidateResult::bookId).containsExactly(10L);
    }

    @Test
    void 대분류나_알_수_없는_code는_400_오류로_거부한다() {
        for (final String code : List.of("novel", "unknown")) {
            assertThatThrownBy(() -> useCase.execute(new GetOnboardingBookCandidatesCommand(List.of(code)))).isInstanceOfSatisfying(
                CoreException.class,
                (final var exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.INVALID_ONBOARDING_SUBCATEGORY_SELECTION));
        }
        verifyNoInteractions(candidateRepository, getBooksUseCase);
    }

    private Book book(final long id) {
        return new Book(id, null, null, "제목" + id, "작가", null, "출판사", "소설", LocalDate.of(2026, 1, 1), null);
    }
}
