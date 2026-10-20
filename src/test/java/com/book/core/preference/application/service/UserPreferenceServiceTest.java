package com.book.core.preference.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.agentchat.application.result.MemoryExtractionTarget;
import com.book.core.agentchat.application.usecase.GetChatHistoryUseCase;
import com.book.core.agentchat.application.usecase.GetMemoryExtractionTargetsUseCase;
import com.book.core.memory.application.command.DeleteUserMemoriesCommand;
import com.book.core.memory.application.result.UserMemoryGroupResult;
import com.book.core.memory.application.usecase.DeleteAllUserMemoriesUseCase;
import com.book.core.memory.application.usecase.DeleteUserMemoriesUseCase;
import com.book.core.memory.application.usecase.ExtractUserMemoriesUseCase;
import com.book.core.memory.application.usecase.GetUserMemoriesUseCase;
import com.book.core.memory.application.usecase.SaveUserMemoriesUseCase;
import com.book.core.memory.domain.UserMemoryType;
import com.book.core.onboarding.application.command.GetOnboardingProgressCommand;
import com.book.core.onboarding.application.result.OnboardingAnswerGroupResult;
import com.book.core.onboarding.application.result.OnboardingOptionResult;
import com.book.core.onboarding.application.result.OnboardingProgressResult;
import com.book.core.onboarding.application.usecase.CheckPersonalizationAgreementUseCase;
import com.book.core.onboarding.application.usecase.GetOnboardingProgressUseCase;
import com.book.core.onboarding.application.usecase.GetProfileRefreshTargetsUseCase;
import com.book.core.onboarding.application.usecase.PersonalizationProfileUseCase;
import com.book.core.onboarding.domain.OnboardingStatus;
import com.book.core.recommendation.application.usecase.RefreshRecommendationFeedUseCase;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.LongStream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class UserPreferenceServiceTest {
    private static final Long USER_ID = 42L;

    private final GetOnboardingProgressUseCase getOnboardingProgressUseCase = mock(GetOnboardingProgressUseCase.class);
    private final GetUserMemoriesUseCase getUserMemoriesUseCase = mock(GetUserMemoriesUseCase.class);
    private final DeleteUserMemoriesUseCase deleteUserMemoriesUseCase = mock(DeleteUserMemoriesUseCase.class);
    private final DeleteAllUserMemoriesUseCase deleteAllUserMemoriesUseCase = mock(DeleteAllUserMemoriesUseCase.class);
    private final GetMemoryExtractionTargetsUseCase getMemoryExtractionTargetsUseCase = mock(GetMemoryExtractionTargetsUseCase.class);
    private final RefreshRecommendationFeedUseCase refreshRecommendationFeedUseCase = mock(RefreshRecommendationFeedUseCase.class);
    private final PersonalizationProfileUseCase personalizationProfileUseCase = mock(PersonalizationProfileUseCase.class);
    private final GetProfileRefreshTargetsUseCase getProfileRefreshTargetsUseCase = mock(GetProfileRefreshTargetsUseCase.class);
    private final GetChatHistoryUseCase getChatHistoryUseCase = mock(GetChatHistoryUseCase.class);
    private final UserPreferenceService userPreferenceService =
        new UserPreferenceService(getOnboardingProgressUseCase, getUserMemoriesUseCase, deleteUserMemoriesUseCase,
            deleteAllUserMemoriesUseCase, mock(CheckPersonalizationAgreementUseCase.class), mock(ExtractUserMemoriesUseCase.class),
            mock(SaveUserMemoriesUseCase.class), getChatHistoryUseCase, getMemoryExtractionTargetsUseCase, personalizationProfileUseCase,
            getProfileRefreshTargetsUseCase, refreshRecommendationFeedUseCase);

    @Test
    void 온보딩을_완료하지_않았으면_취향_조회와_삭제를_진행하지_않고_409를_던진다() {
        when(getOnboardingProgressUseCase.executeCompleted(new GetOnboardingProgressCommand(USER_ID)))
            .thenThrow(new CoreException(ErrorCode.ONBOARDING_NOT_COMPLETED));

        assertNotCompleted(() -> userPreferenceService.getPreferences(USER_ID));
        assertNotCompleted(() -> userPreferenceService.deleteMemories(new DeleteUserMemoriesCommand(USER_ID, List.of(101L))));
        assertNotCompleted(() -> userPreferenceService.deleteAllMemories(USER_ID));
        verifyNoInteractions(getUserMemoriesUseCase, deleteUserMemoriesUseCase, deleteAllUserMemoriesUseCase);
    }

    @Test
    void 온보딩을_완료했으면_선택_삭제와_전체_삭제를_UseCase로_전달한다() {
        final var command = new DeleteUserMemoriesCommand(USER_ID, List.of(101L));
        when(getOnboardingProgressUseCase.executeCompleted(new GetOnboardingProgressCommand(USER_ID)))
            .thenReturn(new OnboardingProgressResult(OnboardingStatus.COMPLETED, null, List.of(), List.of()));

        userPreferenceService.deleteMemories(command);
        userPreferenceService.deleteAllMemories(USER_ID);

        verify(deleteUserMemoriesUseCase).execute(command);
        verify(deleteAllUserMemoriesUseCase).execute(USER_ID);
    }

    @Test
    void 취향_기억을_실제로_지웠을_때만_AI_취향_프로필을_갱신하고_갱신_실패는_삭제_결과에_전파하지_않는다() {
        final var command = new DeleteUserMemoriesCommand(USER_ID, List.of(101L));
        when(getOnboardingProgressUseCase.executeCompleted(new GetOnboardingProgressCommand(USER_ID)))
            .thenReturn(new OnboardingProgressResult(OnboardingStatus.COMPLETED, null, List.of(), List.of()));
        when(deleteUserMemoriesUseCase.execute(command)).thenReturn(1);
        when(deleteAllUserMemoriesUseCase.execute(USER_ID)).thenReturn(0);
        doThrow(new CoreException(ErrorCode.AI_SERVICE_FAILURE)).when(personalizationProfileUseCase).executeUpdate(USER_ID);

        userPreferenceService.deleteMemories(command);
        userPreferenceService.deleteAllMemories(USER_ID);

        verify(personalizationProfileUseCase, times(1)).executeUpdate(USER_ID);
    }

    @Test
    void 야간_프로필_갱신은_기억과_이력이_바뀐_회원을_중복_없이_한_명씩_갱신하고_실패해도_다음_회원을_계속_갱신한다() {
        when(getProfileRefreshTargetsUseCase.execute()).thenReturn(List.of(7L, 3L));
        doThrow(new CoreException(ErrorCode.AI_SERVICE_FAILURE)).when(personalizationProfileUseCase).executeUpdate(3L);

        userPreferenceService.refreshNightlyProfiles(Set.of(7L, 5L));

        final InOrder order = inOrder(personalizationProfileUseCase);
        order.verify(personalizationProfileUseCase).executeUpdate(3L);
        order.verify(personalizationProfileUseCase).executeUpdate(5L);
        order.verify(personalizationProfileUseCase).executeUpdate(7L);
        verify(personalizationProfileUseCase, times(3)).executeUpdate(any());
    }

    @Test
    void 야간_배치는_기억_추출_프로필_갱신_추천_재생성_순으로_실행하고_갱신_대상이_없어도_추천을_다시_뽑는다() {
        when(getMemoryExtractionTargetsUseCase.execute()).thenReturn(List.of());
        when(getProfileRefreshTargetsUseCase.execute()).thenReturn(List.of());
        doThrow(new CoreException(ErrorCode.AI_FEED_FAILURE)).when(refreshRecommendationFeedUseCase).execute();

        userPreferenceService.runNightlyBatch();

        final InOrder order = inOrder(getMemoryExtractionTargetsUseCase, getProfileRefreshTargetsUseCase, refreshRecommendationFeedUseCase);
        order.verify(getMemoryExtractionTargetsUseCase).execute();
        order.verify(getProfileRefreshTargetsUseCase).execute();
        order.verify(refreshRecommendationFeedUseCase).execute();
        verifyNoInteractions(personalizationProfileUseCase);
    }

    @Test
    void 완료한_온보딩_답변과_취향_기억을_함께_반환한다() {
        final var answers = List.of(new OnboardingAnswerGroupResult(3L, List.of(new OnboardingOptionResult(9L, null, "economy", "경제경영"))));
        final var memories = List.of(new UserMemoryGroupResult(List.of(102L, 101L), UserMemoryType.TOPIC, "경제 책을 좋아해요", List.of("경제경영"),
            List.of(LocalDate.of(2026, 10, 16))));
        when(getOnboardingProgressUseCase.executeCompleted(new GetOnboardingProgressCommand(USER_ID)))
            .thenReturn(new OnboardingProgressResult(OnboardingStatus.COMPLETED, null, answers, List.of()));
        when(getUserMemoriesUseCase.execute(USER_ID)).thenReturn(memories);

        final var result = userPreferenceService.getPreferences(USER_ID);

        assertThat(result.onboarding()).isEqualTo(answers);
        assertThat(result.memories()).isEqualTo(memories);
    }

    @Test
    void 추출_대상을_세션_100개나_메시지_1000개_단위로_순서대로_묶어_대화를_조회한다() {
        final List<MemoryExtractionTarget> large =
            List.of(new MemoryExtractionTarget(1L, 1L, 600), new MemoryExtractionTarget(2L, 2L, 1200));
        final List<MemoryExtractionTarget> small =
            LongStream.rangeClosed(3, 103).mapToObj((final long id) -> new MemoryExtractionTarget(id, id, 2)).toList();
        final List<MemoryExtractionTarget> targets = new ArrayList<>(large);
        targets.addAll(small);
        when(getMemoryExtractionTargetsUseCase.execute()).thenReturn(targets);
        when(getChatHistoryUseCase.executeForMemoryExtraction(anyList())).thenReturn(Map.of());

        userPreferenceService.extractNightlyMemories();

        final InOrder order = inOrder(getChatHistoryUseCase);
        order.verify(getChatHistoryUseCase).executeForMemoryExtraction(large.subList(0, 1));
        order.verify(getChatHistoryUseCase).executeForMemoryExtraction(large.subList(1, 2));
        order.verify(getChatHistoryUseCase).executeForMemoryExtraction(small.subList(0, 100));
        order.verify(getChatHistoryUseCase).executeForMemoryExtraction(small.subList(100, 101));
        order.verifyNoMoreInteractions();
    }

    private void assertNotCompleted(final ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(CoreException.class,
            (final CoreException exception) -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.ONBOARDING_NOT_COMPLETED));
    }
}
