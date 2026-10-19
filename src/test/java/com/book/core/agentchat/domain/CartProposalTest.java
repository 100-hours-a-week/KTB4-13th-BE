package com.book.core.agentchat.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class CartProposalTest {
    private static final Instant CREATED = Instant.parse("2026-10-18T00:00:00Z");

    @Test
    void 만료는_리소스_소멸이_아니며_이미_성공한_제안은_계속_성공이다() {
        final CartProposal proposal = proposal();
        assertThat(proposal.executableAt(CREATED.plusSeconds(599))).isTrue();
        assertThat(proposal.executableAt(CREATED.plusSeconds(600))).isFalse();
        assertThat(proposal.stateAt(CREATED.plusSeconds(600))).isEqualTo("EXPIRED");
        assertThat(proposal.items()).hasSize(1);

        final CartProposal applied = proposal();
        applied.consume(10L, CREATED.plusSeconds(599));
        assertThat(applied.stateAt(CREATED.plusSeconds(600))).isEqualTo("EXECUTED");
        assertThatThrownBy(() -> applied.consume(11L, CREATED.plusSeconds(599))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 무효화된_제안은_같은_내용으로_다시_승인할_수_없다() {
        final CartProposal proposal = proposal();
        proposal.invalidate();
        assertThat(proposal.executableAt(CREATED)).isFalse();
        assertThat(proposal.stateAt(CREATED)).isEqualTo("INVALIDATED");
        assertThatThrownBy(() -> proposal.consume(10L, CREATED)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> proposal.items().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    private static CartProposal proposal() {
        return new CartProposal(1L, "source", "20:ADD:2", List.of(new CartChangeLine(CartChangeOperation.ADD, 20L, 0, 2, BigDecimal.TEN)),
            CREATED);
    }
}
