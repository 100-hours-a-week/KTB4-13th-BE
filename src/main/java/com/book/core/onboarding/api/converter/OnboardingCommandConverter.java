package com.book.core.onboarding.api.converter;

import com.book.core.onboarding.api.request.PutOnboardingAnswersRequest;
import com.book.core.onboarding.api.request.PutOnboardingBooksRequest;
import com.book.core.onboarding.application.command.GetOnboardingBookCandidatesCommand;
import com.book.core.onboarding.application.command.GetOnboardingProgressCommand;
import com.book.core.onboarding.application.command.GetOnboardingQuestionCommand;
import com.book.core.onboarding.application.command.PutOnboardingAnswersCommand;
import com.book.core.onboarding.application.command.PutOnboardingBooksCommand;
import com.book.core.onboarding.application.command.RecordPersonalizationAgreementCommand;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OnboardingCommandConverter {
    public GetOnboardingBookCandidatesCommand toGetOnboardingBookCandidatesCommand(final List<String> subcategoryCodes) {
        return new GetOnboardingBookCandidatesCommand(subcategoryCodes);
    }

    public GetOnboardingQuestionCommand toGetOnboardingQuestionCommand(final Long userId, final Long questionId) {
        return new GetOnboardingQuestionCommand(userId, questionId);
    }

    public GetOnboardingProgressCommand toGetOnboardingProgressCommand(final Long userId) {
        return new GetOnboardingProgressCommand(userId);
    }

    public PutOnboardingAnswersCommand toPutOnboardingAnswersCommand(final Long userId, final Long questionId,
        final PutOnboardingAnswersRequest request) {
        return new PutOnboardingAnswersCommand(userId, questionId, request.optionIds());
    }

    public PutOnboardingBooksCommand toPutOnboardingBooksCommand(final Long userId, final PutOnboardingBooksRequest request) {
        return new PutOnboardingBooksCommand(userId, request.bookIds());
    }

    public RecordPersonalizationAgreementCommand toRecordPersonalizationAgreementCommand(final Long userId) {
        return new RecordPersonalizationAgreementCommand(userId);
    }
}
