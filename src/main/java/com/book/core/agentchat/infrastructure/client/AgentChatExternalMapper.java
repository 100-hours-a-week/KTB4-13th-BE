package com.book.core.agentchat.infrastructure.client;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.result.AgentAction;
import com.book.core.agentchat.application.result.AgentButton;
import com.book.core.agentchat.application.result.AgentChosenBook;
import com.book.core.agentchat.application.result.AgentDecision;
import com.book.core.agentchat.application.result.AgentSelection;
import com.book.core.recommendation.application.command.RecommendationCardCandidate;
import com.book.core.recommendation.application.command.ResolveRecommendationCommand;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class AgentChatExternalMapper {
    AgentDecision map(final AgentAiData data) {
        if (data == null || data.reply() == null || data.reply().isBlank() || data.reply().length() > 4000 || data.buttons() == null
            || data.buttons().size() > 10) {
            throw invalid();
        }
        final List<AgentButton> buttons = data.buttons().stream().map(this::button).toList();
        if ("recommend".equals(data.route())) {
            if (data.spec() == null || data.cards() == null || data.degraded() == null
                || (data.actions() != null && !data.actions().isEmpty()) || data.selection() != null
                || buttons.stream().anyMatch((final AgentButton button) -> !"navigate".equals(button.action()))) {
                throw invalid();
            }
            final List<RecommendationCardCandidate> cards = data.cards().stream().map((final AgentAiCard card) -> {
                if (card == null || card.bookId() == null || card.bookId() <= 0) {
                    throw invalid();
                }
                return new RecommendationCardCandidate(card.bookId(), card.matchScore(), card.reasonShort(), card.reasonLong());
            }).toList();
            return new AgentDecision("recommend", data.reply(),
                new ResolveRecommendationCommand(data.spec(), data.reply(), cards, data.followup(), List.of(), data.degraded()), List.of(),
                null, buttons);
        }
        if (!"shop".equals(data.route()) || data.actions() == null || data.actions().size() > 30 || data.spec() != null
            || data.cards() != null || data.followup() != null || data.degraded() != null) {
            throw invalid();
        }
        for (final AgentAiAction action : data.actions()) {
            if (action == null || action.isbn() == null || !action.isbn().matches("[0-9]{13}")) {
                throw invalid();
            }
            if ("set".equals(action.op())) {
                if (action.quantity() == null || action.quantity().compareTo(BigDecimal.ONE) < 0
                    || action.quantity().compareTo(BigDecimal.valueOf(500)) > 0 || action.quantity().stripTrailingZeros().scale() > 0) {
                    throw invalid();
                }
            } else if (!"remove".equals(action.op()) || action.quantity() != null) {
                throw invalid();
            }
        }
        if (data.actions().stream().map(AgentAiAction::isbn).distinct().count() != data.actions().size()) {
            throw invalid();
        }
        AgentSelection selection = null;
        if (data.selection() != null) {
            if (!data.actions().isEmpty() || data.selection().chosen() == null || data.selection().chosen().isEmpty()
                || data.selection().chosen().size() > 30) {
                throw invalid();
            }
            if (buttons.stream().noneMatch((final AgentButton button) -> "confirm_selection".equals(button.action()))) {
                throw invalid();
            }
            final List<AgentChosenBook> chosen = data.selection().chosen();
            if (chosen.stream()
                .anyMatch((final AgentChosenBook book) -> book == null || book.isbn() == null || !book.isbn().matches("[0-9]{13}")
                    || book.price() == null || book.price().signum() < 0)
                || chosen.stream().map(AgentChosenBook::isbn).distinct().count() != chosen.size()) {
                throw invalid();
            }
            final AgentAiConstraints constraints = data.selection().constraints();
            BigDecimal budget = null;
            Integer count = null;
            if (constraints != null) {
                budget = constraints.budgetTotal();
                count = constraints.count();
            }
            selection = new AgentSelection(chosen, budget, count);
            final Set<String> selected = chosen.stream().map(AgentChosenBook::isbn).collect(Collectors.toSet());
            for (final AgentAiButton button : data.buttons()) {
                if ("confirm_selection".equals(button.action())
                    && (button.isbns() == null || button.isbns().stream().anyMatch(Objects::isNull)
                        || button.isbns().size() != selected.size() || !Set.copyOf(button.isbns()).equals(selected))) {
                    throw invalid();
                }
            }
        } else if (buttons.stream().anyMatch((final AgentButton button) -> "confirm_selection".equals(button.action()))) {
            throw invalid();
        }
        final List<AgentAction> actions = data.actions().stream().map((final AgentAiAction action) -> {
            Integer quantity = null;
            if (action.quantity() != null) {
                quantity = action.quantity().intValueExact();
            }
            return new AgentAction(action.op(), action.isbn(), quantity);
        }).toList();
        return new AgentDecision("shop", data.reply(), null, actions, selection, buttons);
    }

    private AgentButton button(final AgentAiButton button) {
        if (button == null || button.label() == null || button.label().isBlank() || button.label().length() > 100) {
            throw invalid();
        }
        if ("navigate".equals(button.action()) && button.target() != null
            && Set.of("CART-001", "ORDER-LIST-001").contains(button.target())) {
            return new AgentButton(button.label(), button.action(), button.target(), null);
        }
        if ("confirm_selection".equals(button.action())) {
            return new AgentButton(button.label(), button.action(), null, null);
        }
        throw invalid();
    }

    private static CoreException invalid() {
        return new CoreException(ErrorCode.AI_SERVICE_FAILURE);
    }
}
