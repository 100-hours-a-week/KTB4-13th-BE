package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.book.application.usecase.FindBooksByIdsUseCase;
import com.book.core.onboarding.application.command.PutOnboardingBooksCommand;
import com.book.core.onboarding.application.port.UserOnboardingBookRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingRepositoryPort;
import com.book.core.onboarding.domain.UserOnboarding;
import com.book.core.onboarding.domain.UserOnboardingBook;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class SaveOnboardingBooksUseCase {
    private final UserOnboardingRepositoryPort userOnboardingRepository;
    private final UserOnboardingBookRepositoryPort onboardingBookRepository;
    private final FindBooksByIdsUseCase findBooksByIdsUseCase;
    private final Clock clock;

    @Transactional
    public void execute(final PutOnboardingBooksCommand command) {
        final UserOnboarding userOnboarding = userOnboardingRepository
                .findActiveByUserId(command.userId())
                .orElseThrow(() -> new CoreException(ErrorCode.ONBOARDING_NOT_FOUND));

        if (!command.bookIds().isEmpty()) {
            final long foundCount =
                    findBooksByIdsUseCase.execute(command.bookIds()).size();
            if (foundCount != command.bookIds().size()) {
                throw new CoreException(ErrorCode.ONBOARDING_BOOK_NOT_FOUND);
            }
        }

        onboardingBookRepository.deleteByUserId(command.userId());
        final List<UserOnboardingBook> newBooks = command.bookIds().stream()
                .map(bookId -> UserOnboardingBook.of(command.userId(), bookId))
                .toList();
        onboardingBookRepository.saveAll(newBooks);

        final LocalDateTime now = LocalDateTime.now(clock);
        userOnboarding.complete(now);
        userOnboardingRepository.save(userOnboarding);
    }
}
