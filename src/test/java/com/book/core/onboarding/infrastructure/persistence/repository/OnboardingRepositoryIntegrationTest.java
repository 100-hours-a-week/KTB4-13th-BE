package com.book.core.onboarding.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.onboarding.application.port.OnboardingBookCandidateRepositoryPort;
import com.book.core.onboarding.application.port.OnboardingOptionRepositoryPort;
import com.book.core.onboarding.application.port.OnboardingQuestionRepositoryPort;
import com.book.core.onboarding.application.port.UserConsentRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingAnswerRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingBookRepositoryPort;
import com.book.core.onboarding.application.port.UserOnboardingRepositoryPort;
import com.book.core.onboarding.domain.ConsentType;
import com.book.core.onboarding.domain.OnboardingBookCandidate;
import com.book.core.onboarding.domain.OnboardingOption;
import com.book.core.onboarding.domain.OnboardingQuestion;
import com.book.core.onboarding.domain.UserConsent;
import com.book.core.onboarding.domain.UserOnboarding;
import com.book.core.onboarding.domain.UserOnboardingAnswer;
import com.book.core.onboarding.domain.UserOnboardingBook;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class OnboardingRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    OnboardingQuestionRepositoryPort questionRepository;

    @Autowired
    OnboardingOptionRepositoryPort optionRepository;

    @Autowired
    UserOnboardingRepositoryPort userOnboardingRepository;

    @Autowired
    UserOnboardingAnswerRepositoryPort answerRepository;

    @Autowired
    UserOnboardingBookRepositoryPort onboardingBookRepository;

    @Autowired
    UserConsentRepositoryPort userConsentRepository;

    @Autowired
    OnboardingBookCandidateRepositoryPort candidateRepository;

    @Autowired
    OnboardingBookCandidateJpaRepository candidateJpaRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void V17_시드된_질문을_순서대로_조회한다() {
        final OnboardingQuestion question1 = questionRepository.findById(1L).orElseThrow();
        final OnboardingQuestion question4 = questionRepository.findById(4L).orElseThrow();

        assertThat(question1.code()).isEqualTo("reading-time");
        assertThat(question1.minSelection()).isEqualTo(1);
        assertThat(question1.maxSelection()).isEqualTo(5);
        assertThat(question4.code()).isEqualTo("subcategory");
        assertThat(questionRepository.findFirstByDisplayOrderGreaterThan(question1.displayOrder()))
            .hasValueSatisfying(next -> assertThat(next.id()).isEqualTo(2L));
        assertThat(questionRepository.findFirstByDisplayOrderGreaterThan(question4.displayOrder())).isEmpty();
    }

    @Test
    void V17_시드된_Q3_옵션과_Q4_옵션의_부모관계를_조회한다() {
        final List<OnboardingOption> q3Options = optionRepository.findByQuestionId(3L);
        final List<OnboardingOption> q4Options = optionRepository.findByQuestionId(4L);

        assertThat(q3Options).hasSize(14);
        assertThat(q3Options).allMatch(option -> option.parentOptionId() == null);
        assertThat(q4Options).hasSize(49);
        assertThat(q4Options).allMatch(option -> option.parentOptionId() != null);

        final OnboardingOption novel = q3Options.stream().filter(option -> option.code().equals("novel")).findFirst().orElseThrow();
        assertThat(q4Options).filteredOn(option -> option.parentOptionId().equals(novel.id())).extracting(OnboardingOption::code)
            .containsExactly("novel-thriller", "novel-sf", "novel-fantasy", "novel-korean", "novel-japanese");
    }

    @Test
    void Q1_같은_질문_안에서_동일한_displayOrder는_제약_예외를_던진다() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO onboarding_options "
            + "(onboarding_questions_id, parent_option_id, code, content, display_order) " + "VALUES (1, NULL, ?, ?, 1)",
            "test-q1-duplicate-order", "테스트")).isInstanceOf(DataAccessException.class);
    }

    @Test
    void Q4_같은_parent_안에서_동일한_displayOrder는_제약_예외를_던진다() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO onboarding_options "
            + "(onboarding_questions_id, parent_option_id, code, content, display_order) " + "VALUES (4, 9, ?, ?, 1)",
            "test-q4-parent9-duplicate-order", "테스트")).isInstanceOf(DataAccessException.class);
    }

    @Test
    void Q4_서로_다른_parent에서는_동일한_displayOrder를_허용한다() {
        jdbc.update("INSERT INTO onboarding_options " + "(onboarding_questions_id, parent_option_id, code, content, display_order) "
            + "VALUES (4, 9, ?, ?, 100)", "test-q4-parent9-order100", "테스트A");
        jdbc.update("INSERT INTO onboarding_options " + "(onboarding_questions_id, parent_option_id, code, content, display_order) "
            + "VALUES (4, 10, ?, ?, 100)", "test-q4-parent10-order100", "테스트B");

        final Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM onboarding_options WHERE code IN (?, ?)", Integer.class,
            "test-q4-parent9-order100", "test-q4-parent10-order100");
        assertThat(count).isEqualTo(2);
    }

    @Test
    void 사용자_온보딩을_저장하고_활성_상태로_조회한다() {
        final Long userId = insertUser("온보딩회원1");
        userOnboardingRepository.save(UserOnboarding.start(userId));

        assertThat(userOnboardingRepository.findActiveByUserId(userId))
            .hasValueSatisfying(userOnboarding -> assertThat(userOnboarding.userId()).isEqualTo(userId));
    }

    @Test
    void 동일_회원의_활성_온보딩은_하나만_허용한다() {
        final Long userId = insertUser("온보딩회원2");
        userOnboardingRepository.save(UserOnboarding.start(userId));

        assertThatThrownBy(() -> jdbc.update("INSERT INTO user_onboardings (user_id, onboarding_status) VALUES (?, 'IN_PROGRESS')", userId))
            .isInstanceOf(DataAccessException.class);
    }

    @Test
    @Transactional
    void 답변을_저장하고_회원별로_조회하며_삭제할_수_있다() {
        final Long userId = insertUser("온보딩회원3");
        answerRepository.saveAll(List.of(UserOnboardingAnswer.of(userId, 1L), UserOnboardingAnswer.of(userId, 2L)));

        assertThat(answerRepository.findByUserId(userId)).extracting(UserOnboardingAnswer::onboardingOptionId).containsExactlyInAnyOrder(1L,
            2L);

        answerRepository.deleteByUserIdAndOnboardingOptionIdIn(userId, List.of(1L, 2L));

        assertThat(answerRepository.findByUserId(userId)).isEmpty();
    }

    @Test
    void 동일_회원의_동일_선택지_중복_답변은_제약_예외를_던진다() {
        final Long userId = insertUser("온보딩회원4");
        answerRepository.saveAll(List.of(UserOnboardingAnswer.of(userId, 1L)));

        assertThatThrownBy(
            () -> jdbc.update("INSERT INTO user_onboarding_answers (user_id, onboarding_option_id) VALUES (?, ?)", userId, 1L))
            .isInstanceOf(DataAccessException.class);
    }

    @Test
    @Transactional
    void 도서_후보_조회_결과의_bookId를_그대로_사용자_선택으로_저장할_수_있다() {
        final Long userId = insertUser("온보딩회원7");
        final Long bookId = insertBook("후보에서_선택된_도서");
        candidateJpaRepository.saveAndFlush(new OnboardingBookCandidate(null, bookId, 1));

        final Long candidateBookId = candidateRepository.findAllOrderByDisplayOrder().stream()
            .filter(candidate -> candidate.bookId().equals(bookId)).findFirst().orElseThrow().bookId();
        onboardingBookRepository.saveAll(List.of(UserOnboardingBook.of(userId, candidateBookId)));

        assertThat(onboardingBookRepository.findByUserId(userId)).extracting(UserOnboardingBook::bookId).containsExactly(bookId);
    }

    @Test
    @Transactional
    void 도서_선택을_저장하고_회원별로_조회하며_삭제할_수_있다() {
        final Long userId = insertUser("온보딩회원5");
        final Long bookId = insertBook("온보딩 통합 테스트 도서 1");

        onboardingBookRepository.saveAll(List.of(UserOnboardingBook.of(userId, bookId)));

        assertThat(onboardingBookRepository.findByUserId(userId)).extracting(UserOnboardingBook::bookId).containsExactly(bookId);

        onboardingBookRepository.deleteByUserId(userId);

        assertThat(onboardingBookRepository.findByUserId(userId)).isEmpty();
    }

    @Test
    void 동일_회원의_동일_도서_중복_선택은_제약_예외를_던진다() {
        final Long userId = insertUser("온보딩회원6");
        final Long bookId = insertBook("온보딩 통합 테스트 도서 2");
        onboardingBookRepository.saveAll(List.of(UserOnboardingBook.of(userId, bookId)));

        assertThatThrownBy(() -> jdbc.update("INSERT INTO user_onboarding_books (user_id, book_id) VALUES (?, ?)", userId, bookId))
            .isInstanceOf(DataAccessException.class);
    }

    @Test
    void 동의를_저장하고_활성_동의를_조회한다() {
        final Long userId = insertUser("동의회원1");
        final LocalDateTime agreedAt = LocalDateTime.of(2026, 1, 1, 0, 0);

        userConsentRepository.save(UserConsent.create(userId, ConsentType.PERSONALIZED_RECOMMENDATION, "v1", agreedAt));

        assertThat(userConsentRepository.findActiveByUserIdAndConsentType(userId, ConsentType.PERSONALIZED_RECOMMENDATION))
            .hasValueSatisfying(consent -> assertThat(consent.agreedAt()).isEqualTo(agreedAt));
    }

    @Test
    void 철회_후_재동의하면_새_행이_생성되고_기존_행은_철회_상태로_DB에_남는다() {
        final Long userId = insertUser("동의회원2");
        final LocalDateTime firstAgreedAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        final UserConsent first =
            userConsentRepository.save(UserConsent.create(userId, ConsentType.PERSONALIZED_RECOMMENDATION, "v1", firstAgreedAt));
        first.withdraw(LocalDateTime.of(2026, 1, 2, 0, 0));
        userConsentRepository.save(first);

        final LocalDateTime secondAgreedAt = LocalDateTime.of(2026, 1, 3, 0, 0);
        userConsentRepository.save(UserConsent.create(userId, ConsentType.PERSONALIZED_RECOMMENDATION, "v1", secondAgreedAt));

        final List<java.util.Map<String, Object>> rows =
            jdbc.queryForList("SELECT agreed_at, withdrawn_at FROM user_consents WHERE user_id = ? ORDER BY id", userId);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).get("withdrawn_at")).isNotNull();
        assertThat(rows.get(1).get("withdrawn_at")).isNull();
    }

    @Test
    void 다른_사용자의_동의는_서로_섞이지_않는다() {
        final Long userId1 = insertUser("동의회원3");
        final Long userId2 = insertUser("동의회원4");
        userConsentRepository
            .save(UserConsent.create(userId1, ConsentType.PERSONALIZED_RECOMMENDATION, "v1", LocalDateTime.of(2026, 1, 1, 0, 0)));

        assertThat(userConsentRepository.findActiveByUserIdAndConsentType(userId1, ConsentType.PERSONALIZED_RECOMMENDATION)).isPresent();
        assertThat(userConsentRepository.findActiveByUserIdAndConsentType(userId2, ConsentType.PERSONALIZED_RECOMMENDATION)).isEmpty();
    }

    @Test
    @Transactional
    void 도서_후보를_display_order_순으로_조회한다() {
        final Long bookId1 = insertBook("후보 도서 1");
        final Long bookId2 = insertBook("후보 도서 2");
        candidateJpaRepository.saveAndFlush(new OnboardingBookCandidate(null, bookId2, 2));
        candidateJpaRepository.saveAndFlush(new OnboardingBookCandidate(null, bookId1, 1));

        final List<Long> ownBookIds = candidateRepository.findAllOrderByDisplayOrder().stream()
            .filter(candidate -> candidate.bookId().equals(bookId1) || candidate.bookId().equals(bookId2))
            .map(OnboardingBookCandidate::bookId).toList();

        assertThat(ownBookIds).containsExactly(bookId1, bookId2);
    }

    @Test
    void 동일_Book에_두번째_후보를_저장하면_UNIQUE_제약을_위반한다() {
        final Long bookId = insertBook("중복 후보 도서");
        candidateJpaRepository.saveAndFlush(new OnboardingBookCandidate(null, bookId, 1));

        assertThatThrownBy(() -> candidateJpaRepository.saveAndFlush(new OnboardingBookCandidate(null, bookId, 2)))
            .isInstanceOf(DataAccessException.class);
    }

    private Long insertUser(final String nickname) {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", nickname);
        return jdbc.queryForObject("SELECT id FROM users WHERE nickname = ?", Long.class, nickname);
    }

    private Long insertBook(final String title) {
        jdbc.update("INSERT INTO books (title, author, publisher, category, published_at) VALUES (?, ?, ?, ?, ?)", title, "저자", "출판사",
            "카테고리", LocalDate.of(2026, 1, 1));
        return jdbc.queryForObject("SELECT id FROM books WHERE title = ?", Long.class, title);
    }
}
