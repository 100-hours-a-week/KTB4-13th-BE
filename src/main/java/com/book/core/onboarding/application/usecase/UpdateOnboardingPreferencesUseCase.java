package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.book.application.usecase.GetBooksUseCase;
import com.book.core.onboarding.application.command.OnboardingPreferenceAnswerCommand;
import com.book.core.onboarding.application.command.PutOnboardingPreferencesCommand;
import com.book.core.onboarding.application.port.OnboardingOptionRepositoryPort;
import com.book.core.onboarding.application.port.OnboardingQuestionRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingAnswerRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingBookRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingRepositoryPort;
import com.book.core.onboarding.domain.OnboardingOption;
import com.book.core.onboarding.domain.OnboardingQuestion;
import com.book.core.onboarding.domain.OnboardingStatus;
import com.book.core.onboarding.domain.UserOnboarding;
import com.book.core.onboarding.domain.UserOnboardingAnswer;
import com.book.core.onboarding.domain.UserOnboardingBook;
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
public class UpdateOnboardingPreferencesUseCase {
    private final UserOnboardingRepositoryPort userOnboardingRepository;
    private final OnboardingQuestionRepositoryPort questionRepository;
    private final OnboardingOptionRepositoryPort optionRepository;
    private final UserOnboardingAnswerRepositoryPort answerRepository;
    private final UserOnboardingBookRepositoryPort bookRepository;
    private final GetBooksUseCase getBooksUseCase;

    @Transactional
    public void execute(final PutOnboardingPreferencesCommand command) {
        validateCompleted(command.userId());
        validateAnswers(command.answers());
        validateBooks(command.bookIds());

        final List<Long> requestedOptionIds = command.answers().stream().flatMap(answer -> answer.optionIds().stream()).toList();
        final List<Long> currentOptionIds =
            answerRepository.findByUserId(command.userId()).stream().map(UserOnboardingAnswer::onboardingOptionId).toList();
        replaceAnswers(command.userId(), currentOptionIds, requestedOptionIds);

        final List<Long> currentBookIds = bookRepository.findByUserId(command.userId()).stream().map(UserOnboardingBook::bookId).toList();
        replaceBooks(command.userId(), currentBookIds, command.bookIds());
    }

    private void validateCompleted(final Long userId) {
        final UserOnboarding userOnboarding =
            userOnboardingRepository.findActiveByUserId(userId).orElseThrow(() -> new CoreException(ErrorCode.ONBOARDING_NOT_FOUND));
        if (userOnboarding.onboardingStatus() != OnboardingStatus.COMPLETED) {
            throw new CoreException(ErrorCode.ONBOARDING_NOT_COMPLETED);
        }
    }

    private void validateAnswers(final List<OnboardingPreferenceAnswerCommand> answers) {
        final Map<Long, OnboardingQuestion> questionsById =
            questionRepository.findAll().stream().collect(Collectors.toMap(OnboardingQuestion::id, Function.identity()));
        if (questionsById.isEmpty()) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_PREFERENCES);
        }
        final Set<Long> requestedQuestionIds =
            answers.stream().map(OnboardingPreferenceAnswerCommand::questionId).collect(Collectors.toSet());
        if (!requestedQuestionIds.equals(questionsById.keySet())) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_PREFERENCES);
        }
        answers.forEach(answer -> validateSelectionCount(questionsById.get(answer.questionId()), answer.optionIds()));

        final List<Long> requestedOptionIds = answers.stream().flatMap(answer -> answer.optionIds().stream()).distinct().toList();
        final Map<Long, OnboardingOption> optionsById = optionRepository.findAllByIdIn(requestedOptionIds).stream()
            .collect(Collectors.toMap(OnboardingOption::id, Function.identity()));
        if (optionsById.size() != requestedOptionIds.size()) {
            throw new CoreException(ErrorCode.ONBOARDING_OPTION_NOT_FOUND);
        }
        final boolean mismatched = answers.stream().anyMatch(answer -> answer.optionIds().stream()
            .anyMatch(optionId -> !optionsById.get(optionId).onboardingQuestionId().equals(answer.questionId())));
        if (mismatched) {
            throw new CoreException(ErrorCode.ONBOARDING_OPTION_QUESTION_MISMATCH);
        }

        final boolean parentMissing = optionsById.values().stream().map(OnboardingOption::parentOptionId).filter(Objects::nonNull)
            .anyMatch(parentOptionId -> !optionsById.containsKey(parentOptionId));
        if (parentMissing) {
            throw new CoreException(ErrorCode.ONBOARDING_PARENT_QUESTION_NOT_ANSWERED);
        }
    }

    private void validateSelectionCount(final OnboardingQuestion question, final List<Long> optionIds) {
        if (optionIds.size() < question.minSelection()) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION);
        }
        if (question.maxSelection() != null && optionIds.size() > question.maxSelection()) {
            throw new CoreException(ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION);
        }
    }

    private void validateBooks(final List<Long> bookIds) {
        if (bookIds.isEmpty()) {
            return;
        }
        if (getBooksUseCase.execute(bookIds).size() != bookIds.size()) {
            throw new CoreException(ErrorCode.ONBOARDING_BOOK_NOT_FOUND);
        }
    }

    private void replaceAnswers(final Long userId, final List<Long> currentOptionIds, final List<Long> requestedOptionIds) {
        final List<Long> removedOptionIds = difference(currentOptionIds, requestedOptionIds);
        if (!removedOptionIds.isEmpty()) {
            answerRepository.deleteByUserIdAndOnboardingOptionIdIn(userId, removedOptionIds);
        }
        final List<Long> addedOptionIds = difference(requestedOptionIds, currentOptionIds);
        if (!addedOptionIds.isEmpty()) {
            answerRepository.saveAll(addedOptionIds.stream().map(optionId -> UserOnboardingAnswer.of(userId, optionId)).toList());
        }
    }

    private void replaceBooks(final Long userId, final List<Long> currentBookIds, final List<Long> requestedBookIds) {
        final List<Long> removedBookIds = difference(currentBookIds, requestedBookIds);
        if (!removedBookIds.isEmpty()) {
            bookRepository.deleteByUserIdAndBookIdIn(userId, removedBookIds);
        }
        final List<Long> addedBookIds = difference(requestedBookIds, currentBookIds);
        if (!addedBookIds.isEmpty()) {
            bookRepository.saveAll(addedBookIds.stream().map(bookId -> UserOnboardingBook.of(userId, bookId)).toList());
        }
    }

    private static List<Long> difference(final List<Long> source, final List<Long> excluded) {
        final Set<Long> excludedIds = Set.copyOf(excluded);
        return source.stream().filter(id -> !excludedIds.contains(id)).distinct().toList();
    }
}
