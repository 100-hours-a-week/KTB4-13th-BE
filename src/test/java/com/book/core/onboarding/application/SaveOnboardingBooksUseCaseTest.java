package com.book.core.onboarding.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.application.usecase.FindBooksByIdsUseCase;
import com.book.core.book.domain.Book;
import com.book.core.onboarding.application.command.PutOnboardingBooksCommand;
import com.book.core.onboarding.application.port.UserOnboardingBookRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingRepositoryPort;
import com.book.core.onboarding.application.usecase.SaveOnboardingBooksUseCase;
import com.book.core.onboarding.domain.OnboardingStatus;
import com.book.core.onboarding.domain.UserOnboarding;
import com.book.core.onboarding.domain.UserOnboardingBook;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SaveOnboardingBooksUseCaseTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    private final FakeUserOnboardingRepository userOnboardingRepository = new FakeUserOnboardingRepository();
    private final FakeUserOnboardingBookRepository onboardingBookRepository = new FakeUserOnboardingBookRepository();
    private final FakeBookRepository bookRepository = new FakeBookRepository();
    private final FindBooksByIdsUseCase findBooksByIdsUseCase = new FindBooksByIdsUseCase(bookRepository);
    private final SaveOnboardingBooksUseCase useCase = new SaveOnboardingBooksUseCase(
            userOnboardingRepository, onboardingBookRepository, findBooksByIdsUseCase, FIXED_CLOCK);

    @Test
    void 존재하는_도서를_선택하면_저장한다() {
        userOnboardingRepository.byUserId.put(42L, UserOnboarding.start(42L));
        bookRepository.booksById.put(10L, book(10L));

        useCase.execute(new PutOnboardingBooksCommand(42L, List.of(10L)));

        assertThat(onboardingBookRepository.savedBooks)
                .extracting(UserOnboardingBook::bookId)
                .containsExactly(10L);
    }

    @Test
    void 기존_선택을_새_선택으로_교체한다() {
        userOnboardingRepository.byUserId.put(42L, UserOnboarding.start(42L));
        bookRepository.booksById.put(20L, book(20L));

        useCase.execute(new PutOnboardingBooksCommand(42L, List.of(20L)));

        assertThat(onboardingBookRepository.deletedUserIds).containsExactly(42L);
        assertThat(onboardingBookRepository.savedBooks)
                .extracting(UserOnboardingBook::bookId)
                .containsExactly(20L);
    }

    @Test
    void 빈_배열이면_기존_선택을_모두_지운다() {
        userOnboardingRepository.byUserId.put(42L, UserOnboarding.start(42L));

        useCase.execute(new PutOnboardingBooksCommand(42L, List.of()));

        assertThat(onboardingBookRepository.deletedUserIds).containsExactly(42L);
        assertThat(onboardingBookRepository.savedBooks).isEmpty();
    }

    @Test
    void 중복된_도서이면_커맨드_생성_시점에_예외를_던진다() {
        assertThatThrownBy(() -> new PutOnboardingBooksCommand(42L, List.of(10L, 10L)))
                .isInstanceOf(CoreException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_ONBOARDING_BOOK_SELECTION);
    }

    @Test
    void 존재하지_않는_도서가_포함되면_예외를_던진다() {
        userOnboardingRepository.byUserId.put(42L, UserOnboarding.start(42L));

        assertThatThrownBy(() -> useCase.execute(new PutOnboardingBooksCommand(42L, List.of(999L))))
                .isInstanceOf(CoreException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ONBOARDING_BOOK_NOT_FOUND);
    }

    @Test
    void 진행중인_온보딩이_없으면_예외를_던진다() {
        assertThatThrownBy(() -> useCase.execute(new PutOnboardingBooksCommand(42L, List.of())))
                .isInstanceOf(CoreException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ONBOARDING_NOT_FOUND);
    }

    @Test
    void 도서_저장에_성공하면_온보딩_상태를_완료로_전환한다() {
        userOnboardingRepository.byUserId.put(42L, UserOnboarding.start(42L));
        bookRepository.booksById.put(10L, book(10L));

        useCase.execute(new PutOnboardingBooksCommand(42L, List.of(10L)));

        final UserOnboarding saved = userOnboardingRepository.byUserId.get(42L);
        assertThat(saved.onboardingStatus()).isEqualTo(OnboardingStatus.COMPLETED);
        assertThat(saved.completedAt())
                .isEqualTo(LocalDateTime.ofInstant(FIXED_CLOCK.instant(), FIXED_CLOCK.getZone()));
    }

    @Test
    void 완료_시각은_최초_1회만_설정된다() {
        final LocalDateTime firstCompletedAt = LocalDateTime.of(2025, 6, 1, 0, 0);
        userOnboardingRepository.byUserId.put(
                42L, new UserOnboarding(1L, 42L, OnboardingStatus.COMPLETED, firstCompletedAt));

        useCase.execute(new PutOnboardingBooksCommand(42L, List.of()));

        final UserOnboarding saved = userOnboardingRepository.byUserId.get(42L);
        assertThat(saved.completedAt()).isEqualTo(firstCompletedAt);
        assertThat(saved.onboardingStatus()).isEqualTo(OnboardingStatus.COMPLETED);
    }

    private Book book(final Long id) {
        return new Book(id, null, null, "제목", "저자", null, "출판사", "카테고리", null, null);
    }

    private static final class FakeUserOnboardingRepository implements UserOnboardingRepositoryPort {
        private final Map<Long, UserOnboarding> byUserId = new HashMap<>();

        @Override
        public Optional<UserOnboarding> findActiveByUserId(final Long userId) {
            return Optional.ofNullable(byUserId.get(userId));
        }

        @Override
        public UserOnboarding save(final UserOnboarding userOnboarding) {
            byUserId.put(userOnboarding.userId(), userOnboarding);
            return userOnboarding;
        }
    }

    private static final class FakeUserOnboardingBookRepository implements UserOnboardingBookRepositoryPort {
        private final List<Long> deletedUserIds = new ArrayList<>();
        private List<UserOnboardingBook> savedBooks = List.of();

        @Override
        public List<UserOnboardingBook> findByUserId(final Long userId) {
            return new ArrayList<>();
        }

        @Override
        public void deleteByUserId(final Long userId) {
            deletedUserIds.add(userId);
        }

        @Override
        public List<UserOnboardingBook> saveAll(final List<UserOnboardingBook> books) {
            savedBooks = books;
            return books;
        }
    }

    private static final class FakeBookRepository implements BookRepositoryPort {
        private final Map<Long, Book> booksById = new HashMap<>();

        @Override
        public List<Book> findAllByIdIn(final List<Long> ids) {
            return ids.stream()
                    .map(booksById::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }
    }
}
