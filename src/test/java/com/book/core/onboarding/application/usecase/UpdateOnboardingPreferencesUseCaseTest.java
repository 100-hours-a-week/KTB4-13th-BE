package com.book.core.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.book.application.port.BookRepositoryPort;
import com.book.core.book.application.usecase.GetBooksUseCase;
import com.book.core.book.domain.Book;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateOnboardingPreferencesUseCaseTest {
    private static final Long USER_ID = 42L;
    private static final LocalDateTime COMPLETED_AT = LocalDateTime.of(2026, 1, 1, 0, 0);

    private final FakeUserOnboardingRepository userOnboardingRepository = new FakeUserOnboardingRepository();
    private final FakeOnboardingQuestionRepository questionRepository = new FakeOnboardingQuestionRepository();
    private final FakeOnboardingOptionRepository optionRepository = new FakeOnboardingOptionRepository();
    private final FakeUserOnboardingAnswerRepository answerRepository = new FakeUserOnboardingAnswerRepository();
    private final FakeUserOnboardingBookRepository bookRepository = new FakeUserOnboardingBookRepository();
    private final FakeBookRepository catalogRepository = new FakeBookRepository();
    private final UpdateOnboardingPreferencesUseCase useCase = new UpdateOnboardingPreferencesUseCase(userOnboardingRepository,
        questionRepository, optionRepository, answerRepository, bookRepository, new GetBooksUseCase(catalogRepository));

    @BeforeEach
    void setUp() {
        questionRepository.add(new OnboardingQuestion(1L, "reading-time", "Q1", 1, 5, 1));
        questionRepository.add(new OnboardingQuestion(2L, "book-criteria", "Q2", 1, 3, 2));
        questionRepository.add(new OnboardingQuestion(3L, "main-category", "Q3", 1, 3, 3));
        questionRepository.add(new OnboardingQuestion(4L, "subcategory", "Q4", 1, 9, 4));
        optionRepository.add(new OnboardingOption(1L, 1L, null, "morning", "아침", 1));
        optionRepository.add(new OnboardingOption(2L, 1L, null, "lunch", "점심", 2));
        optionRepository.add(new OnboardingOption(6L, 2L, null, "publisher", "출판사", 1));
        optionRepository.add(new OnboardingOption(9L, 3L, null, "novel", "소설", 1));
        optionRepository.add(new OnboardingOption(10L, 3L, null, "humanities", "인문", 2));
        optionRepository.add(new OnboardingOption(23L, 4L, 9L, "novel-thriller", "추리/스릴러", 1));
        optionRepository.add(new OnboardingOption(28L, 4L, 10L, "humanities-psychology", "심리학", 1));
        List.of(100L, 200L, 300L).forEach(catalogRepository::add);

        userOnboardingRepository.put(new UserOnboarding(1L, USER_ID, OnboardingStatus.COMPLETED, COMPLETED_AT));
        answerRepository.answers.addAll(List.of(1L, 6L, 9L, 23L).stream().map(id -> UserOnboardingAnswer.of(USER_ID, id)).toList());
        bookRepository.books.addAll(List.of(100L, 200L).stream().map(id -> UserOnboardingBook.of(USER_ID, id)).toList());
    }

    @Test
    void 기존_답변과_도서에서_변경된_항목만_삭제하고_추가한다() {
        final boolean changed = useCase
            .execute(command(List.of(answer(1L, 2L), answer(2L, 6L), answer(3L, 9L, 10L), answer(4L, 23L, 28L)), List.of(200L, 300L)));

        assertThat(changed).isTrue();
        assertThat(answerRepository.deletedOptionIds).containsExactly(List.of(1L));
        assertThat(answerRepository.savedOptionIds).containsExactly(List.of(2L, 10L, 28L));
        assertThat(answerRepository.optionIds()).containsExactlyInAnyOrder(2L, 6L, 9L, 10L, 23L, 28L);
        assertThat(bookRepository.deletedBookIds).containsExactly(List.of(100L));
        assertThat(bookRepository.savedBookIds).containsExactly(List.of(300L));
        assertThat(bookRepository.bookIds()).containsExactlyInAnyOrder(200L, 300L);
    }

    @Test
    void 변경이_없으면_성공하고_저장소_쓰기를_생략한다() {
        final boolean changed = useCase.execute(currentPreferences(List.of(100L, 200L)));

        assertThat(changed).isFalse();
        assertThat(answerRepository.deletedOptionIds).isEmpty();
        assertThat(answerRepository.savedOptionIds).isEmpty();
        assertThat(bookRepository.deletedBookIds).isEmpty();
        assertThat(bookRepository.savedBookIds).isEmpty();
    }

    @Test
    void 답변만_변경하면_변경됨을_반환한다() {
        final boolean changed =
            useCase.execute(command(List.of(answer(1L, 2L), answer(2L, 6L), answer(3L, 9L), answer(4L, 23L)), List.of(100L, 200L)));

        assertThat(changed).isTrue();
        assertThat(answerRepository.savedOptionIds).containsExactly(List.of(2L));
        assertThat(bookRepository.deletedBookIds).isEmpty();
        assertThat(bookRepository.savedBookIds).isEmpty();
    }

    @Test
    void 도서만_비우면_기존_도서를_모두_삭제하고_변경됨을_반환한다() {
        final boolean changed = useCase.execute(currentPreferences(List.of()));

        assertThat(changed).isTrue();
        assertThat(answerRepository.deletedOptionIds).isEmpty();
        assertThat(answerRepository.savedOptionIds).isEmpty();
        assertThat(bookRepository.deletedBookIds).containsExactly(List.of(100L, 200L));
        assertThat(bookRepository.savedBookIds).isEmpty();
        assertThat(bookRepository.bookIds()).isEmpty();
    }

    @Test
    void 온보딩_기록이_없으면_예외를_던진다() {
        userOnboardingRepository.byUserId.clear();

        assertError(currentPreferences(List.of()), ErrorCode.ONBOARDING_NOT_FOUND);
    }

    @Test
    void 완료되지_않은_온보딩이면_수정하지_않고_예외를_던진다() {
        userOnboardingRepository.put(UserOnboarding.start(USER_ID));

        assertError(currentPreferences(List.of()), ErrorCode.ONBOARDING_NOT_COMPLETED);
        assertNoWrites();
    }

    @Test
    void 질문이_누락되거나_존재하지_않는_질문이_포함되면_예외를_던진다() {
        assertError(command(List.of(answer(1L, 1L), answer(2L, 6L), answer(3L, 9L)), List.of()), ErrorCode.INVALID_ONBOARDING_PREFERENCES);
        assertError(command(List.of(answer(1L, 1L), answer(2L, 6L), answer(3L, 9L), answer(4L, 23L), answer(99L, 1L)), List.of()),
            ErrorCode.INVALID_ONBOARDING_PREFERENCES);
        assertNoWrites();
    }

    @Test
    void 질문별_최소_선택_개수_미만이거나_최대_선택_개수를_초과하면_예외를_던진다() {
        List.of(7L, 8L, 2004L).forEach(
            optionId -> optionRepository.add(new OnboardingOption(optionId, 2L, null, "code-" + optionId, "선택지", optionId.intValue())));

        assertError(command(List.of(answer(1L), answer(2L, 6L), answer(3L, 9L), answer(4L, 23L)), List.of()),
            ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION);
        assertError(command(List.of(answer(1L, 1L), answer(2L, 6L, 7L, 8L, 2004L), answer(3L, 9L), answer(4L, 23L)), List.of()),
            ErrorCode.INVALID_ONBOARDING_ANSWER_SELECTION);
        assertNoWrites();
    }

    @Test
    void 존재하지_않는_선택지가_포함되면_예외를_던진다() {
        assertError(command(List.of(answer(1L, 999L), answer(2L, 6L), answer(3L, 9L), answer(4L, 23L)), List.of()),
            ErrorCode.ONBOARDING_OPTION_NOT_FOUND);
        assertNoWrites();
    }

    @Test
    void 선택지가_다른_질문에_속하면_예외를_던진다() {
        assertError(command(List.of(answer(1L, 6L), answer(2L, 6L), answer(3L, 9L), answer(4L, 23L)), List.of()),
            ErrorCode.ONBOARDING_OPTION_QUESTION_MISMATCH);
        assertNoWrites();
    }

    @Test
    void Q4_선택지의_부모가_이번_요청의_Q3에_없으면_저장된_Q3_답변이나_기존_Q4와_무관하게_예외를_던진다() {
        answerRepository.answers.add(UserOnboardingAnswer.of(USER_ID, 10L));

        assertError(command(List.of(answer(1L, 1L), answer(2L, 6L), answer(3L, 9L), answer(4L, 28L)), List.of()),
            ErrorCode.ONBOARDING_PARENT_QUESTION_NOT_ANSWERED);
        assertError(command(List.of(answer(1L, 1L), answer(2L, 6L), answer(3L, 10L), answer(4L, 23L)), List.of()),
            ErrorCode.ONBOARDING_PARENT_QUESTION_NOT_ANSWERED);
        assertNoWrites();
    }

    @Test
    void 등록된_질문이_없으면_빈_답변_요청도_거부한다() {
        questionRepository.questions.clear();

        assertError(command(List.of(), List.of()), ErrorCode.INVALID_ONBOARDING_PREFERENCES);
        assertNoWrites();
    }

    @Test
    void 존재하지_않는_도서가_포함되면_예외를_던진다() {
        assertError(currentPreferences(List.of(200L, 999L)), ErrorCode.ONBOARDING_BOOK_NOT_FOUND);
        assertNoWrites();
    }

    private PutOnboardingPreferencesCommand currentPreferences(final List<Long> bookIds) {
        return command(currentAnswers(), bookIds);
    }

    private List<OnboardingPreferenceAnswerCommand> currentAnswers() {
        return List.of(answer(1L, 1L), answer(2L, 6L), answer(3L, 9L), answer(4L, 23L));
    }

    private PutOnboardingPreferencesCommand command(final List<OnboardingPreferenceAnswerCommand> answers, final List<Long> bookIds) {
        return new PutOnboardingPreferencesCommand(USER_ID, answers, bookIds);
    }

    private OnboardingPreferenceAnswerCommand answer(final Long questionId, final Long... optionIds) {
        return new OnboardingPreferenceAnswerCommand(questionId, List.of(optionIds));
    }

    private void assertError(final PutOnboardingPreferencesCommand command, final ErrorCode errorCode) {
        assertThatThrownBy(() -> useCase.execute(command)).isInstanceOf(CoreException.class).extracting("errorCode").isEqualTo(errorCode);
    }

    private void assertNoWrites() {
        assertThat(answerRepository.deletedOptionIds).isEmpty();
        assertThat(answerRepository.savedOptionIds).isEmpty();
        assertThat(bookRepository.deletedBookIds).isEmpty();
        assertThat(bookRepository.savedBookIds).isEmpty();
    }

    private static final class FakeUserOnboardingRepository implements UserOnboardingRepositoryPort {
        private final Map<Long, UserOnboarding> byUserId = new HashMap<>();

        private void put(final UserOnboarding userOnboarding) {
            byUserId.put(userOnboarding.userId(), userOnboarding);
        }

        @Override
        public Optional<UserOnboarding> findActiveByUserId(final Long userId) {
            return Optional.ofNullable(byUserId.get(userId));
        }

        @Override
        public UserOnboarding save(final UserOnboarding userOnboarding) {
            put(userOnboarding);
            return userOnboarding;
        }
    }

    private static final class FakeOnboardingQuestionRepository implements OnboardingQuestionRepositoryPort {
        private final Map<Long, OnboardingQuestion> questions = new HashMap<>();

        private void add(final OnboardingQuestion question) {
            questions.put(question.id(), question);
        }

        @Override
        public Optional<OnboardingQuestion> findById(final Long id) {
            return Optional.ofNullable(questions.get(id));
        }

        @Override
        public List<OnboardingQuestion> findAll() {
            return List.copyOf(questions.values());
        }

        @Override
        public Optional<OnboardingQuestion> findFirstByDisplayOrderGreaterThan(final int displayOrder) {
            return Optional.empty();
        }
    }

    private static final class FakeOnboardingOptionRepository implements OnboardingOptionRepositoryPort {
        private final Map<Long, OnboardingOption> options = new HashMap<>();

        private void add(final OnboardingOption option) {
            options.put(option.id(), option);
        }

        @Override
        public List<OnboardingOption> findByQuestionId(final Long questionId) {
            return options.values().stream().filter(option -> option.onboardingQuestionId().equals(questionId)).toList();
        }

        @Override
        public List<OnboardingOption> findAllByIdIn(final List<Long> ids) {
            return ids.stream().map(options::get).filter(Objects::nonNull).toList();
        }
    }

    private static final class FakeUserOnboardingAnswerRepository implements UserOnboardingAnswerRepositoryPort {
        private final List<UserOnboardingAnswer> answers = new ArrayList<>();
        private final List<List<Long>> deletedOptionIds = new ArrayList<>();
        private final List<List<Long>> savedOptionIds = new ArrayList<>();

        private List<Long> optionIds() {
            return answers.stream().map(UserOnboardingAnswer::onboardingOptionId).toList();
        }

        @Override
        public List<UserOnboardingAnswer> findByUserId(final Long userId) {
            return answers.stream().filter(answer -> answer.userId().equals(userId)).toList();
        }

        @Override
        public void deleteByUserIdAndOnboardingOptionIdIn(final Long userId, final List<Long> onboardingOptionIds) {
            deletedOptionIds.add(onboardingOptionIds);
            answers.removeIf(answer -> answer.userId().equals(userId) && onboardingOptionIds.contains(answer.onboardingOptionId()));
        }

        @Override
        public List<UserOnboardingAnswer> saveAll(final List<UserOnboardingAnswer> newAnswers) {
            savedOptionIds.add(newAnswers.stream().map(UserOnboardingAnswer::onboardingOptionId).toList());
            answers.addAll(newAnswers);
            return newAnswers;
        }
    }

    private static final class FakeUserOnboardingBookRepository implements UserOnboardingBookRepositoryPort {
        private final List<UserOnboardingBook> books = new ArrayList<>();
        private final List<List<Long>> deletedBookIds = new ArrayList<>();
        private final List<List<Long>> savedBookIds = new ArrayList<>();

        private List<Long> bookIds() {
            return books.stream().map(UserOnboardingBook::bookId).toList();
        }

        @Override
        public List<UserOnboardingBook> findByUserId(final Long userId) {
            return books.stream().filter(book -> book.userId().equals(userId)).toList();
        }

        @Override
        public void deleteByUserId(final Long userId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteByUserIdAndBookIdIn(final Long userId, final List<Long> bookIds) {
            deletedBookIds.add(bookIds);
            books.removeIf(book -> book.userId().equals(userId) && bookIds.contains(book.bookId()));
        }

        @Override
        public List<UserOnboardingBook> saveAll(final List<UserOnboardingBook> newBooks) {
            savedBookIds.add(newBooks.stream().map(UserOnboardingBook::bookId).toList());
            books.addAll(newBooks);
            return newBooks;
        }
    }

    private static final class FakeBookRepository implements BookRepositoryPort {
        private final Map<Long, Book> booksById = new HashMap<>();

        private void add(final Long id) {
            booksById.put(id, new Book(id, null, null, "제목", "저자", null, "출판사", "카테고리", null, null));
        }

        @Override
        public List<Book> findAllByIdIn(final List<Long> ids) {
            return ids.stream().map(booksById::get).filter(Objects::nonNull).toList();
        }
    }
}
