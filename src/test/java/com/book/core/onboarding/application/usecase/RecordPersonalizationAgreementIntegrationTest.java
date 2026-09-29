package com.book.core.onboarding.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.book.core.onboarding.application.command.RecordPersonalizationAgreementCommand;
import com.book.core.onboarding.application.port.TermRepositoryPort;
import com.book.core.onboarding.application.port.UserTermAgreementRepositoryPort;
import com.book.core.onboarding.domain.Term;
import com.book.core.onboarding.domain.TermType;
import com.book.core.onboarding.domain.UserTermAgreement;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Tag("integration")
class RecordPersonalizationAgreementIntegrationTest {
    @Container
    @ServiceConnection
    static final MySQLContainer mysql = new MySQLContainer("mysql:8.4.8");

    @Autowired
    RecordPersonalizationAgreementUseCase useCase;

    @Autowired
    TermRepositoryPort termRepository;

    @Autowired
    UserTermAgreementRepositoryPort agreementRepository;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void 활성_개인화_약관이_시드된다() {
        final Term term = termRepository.findActiveByTermType(TermType.PERSONALIZED_RECOMMENDATION).orElseThrow();

        assertThat(term.title()).isEqualTo("개인화 도서 추천을 위한 정보 수집·이용 동의");
        assertThat(term.content()).isEqualTo("이용 목적: 독서 취향 분석 및 개인화 도서 추천 제공\n수집·이용 항목: 온보딩 질문 응답, 선택한 관심 도서");
        assertThat(term.version()).isEqualTo("1.0");
        assertThat(term.isRequired()).isFalse();
        assertThat(term.displayOrder()).isEqualTo(1);
    }

    @Test
    void 같은_사용자와_약관의_AGREE_이력을_다시_저장하면_UNIQUE_제약으로_거부한다() {
        final Long userId = insertUser("개인화동의회원1");
        final Long termId = termRepository.findActiveByTermType(TermType.PERSONALIZED_RECOMMENDATION).orElseThrow().id();
        agreementRepository.save(UserTermAgreement.agree(userId, termId));

        assertThatThrownBy(() -> agreementRepository.save(UserTermAgreement.agree(userId, termId)))
            .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(agreementCount(userId)).isEqualTo(1);
    }

    @Test
    void 같은_사용자의_동시_요청은_모두_성공하고_이력은_한_건만_남는다() throws Exception {
        final Long userId = insertUser("개인화동의회원2");
        final int requestCount = 8;
        final CyclicBarrier barrier = new CyclicBarrier(requestCount);
        final Callable<Void> request = () -> {
            barrier.await();
            useCase.execute(new RecordPersonalizationAgreementCommand(userId));
            return null;
        };

        final ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        try {
            final List<Future<Void>> results = executor.invokeAll(Collections.nCopies(requestCount, request));
            for (final Future<Void> result : results) {
                assertThatCode(result::get).doesNotThrowAnyException();
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(agreementCount(userId)).isEqualTo(1);
    }

    private Long insertUser(final String nickname) {
        jdbc.update("INSERT INTO users (nickname) VALUES (?)", nickname);
        return jdbc.queryForObject("SELECT id FROM users WHERE nickname = ?", Long.class, nickname);
    }

    private int agreementCount(final Long userId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM user_term_agreement WHERE user_id = ?", Integer.class, userId);
    }
}
