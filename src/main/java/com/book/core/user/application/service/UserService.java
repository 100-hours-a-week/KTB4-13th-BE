package com.book.core.user.application.service;

import com.book.core.library.application.usecase.DeleteAllLibraryBooksUseCase;
import com.book.core.onboarding.application.usecase.DeleteUserOnboardingDataUseCase;
import com.book.core.onboarding.application.usecase.RequestProfileSyncUseCase;
import com.book.core.user.application.command.UpdateMyInfoCommand;
import com.book.core.user.application.result.GetMyInfoResult;
import com.book.core.user.application.usecase.CompleteUserDeletionUseCase;
import com.book.core.user.application.usecase.FindWithdrawalTargetsUseCase;
import com.book.core.user.application.usecase.FindWithdrawalUnlinkProvidersUseCase;
import com.book.core.user.application.usecase.GetMyInfoUseCase;
import com.book.core.user.application.usecase.RequestUserDeletionUseCase;
import com.book.core.user.application.usecase.UnlinkWithdrawalProvidersUseCase;
import com.book.core.user.application.usecase.UpdateMyInfoUseCase;
import com.book.core.user.domain.UserProvider;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final GetMyInfoUseCase getMyInfoUseCase;
    private final UpdateMyInfoUseCase updateMyInfoUseCase;
    private final RequestUserDeletionUseCase requestUserDeletionUseCase;
    private final FindWithdrawalTargetsUseCase findWithdrawalTargetsUseCase;
    private final FindWithdrawalUnlinkProvidersUseCase findWithdrawalUnlinkProvidersUseCase;
    private final UnlinkWithdrawalProvidersUseCase unlinkWithdrawalProvidersUseCase;
    private final DeleteUserOnboardingDataUseCase deleteUserOnboardingDataUseCase;
    private final DeleteAllLibraryBooksUseCase deleteAllLibraryBooksUseCase;
    private final CompleteUserDeletionUseCase completeUserDeletionUseCase;
    private final RequestProfileSyncUseCase requestProfileSyncUseCase;

    public GetMyInfoResult getMyInfo(final Long userId) {
        return getMyInfoUseCase.execute(userId);
    }

    public void updateMyInfo(final UpdateMyInfoCommand command) {
        updateMyInfoUseCase.execute(command);
    }

    public void requestUserDeletion(final Long userId) {
        requestUserDeletionUseCase.execute(userId);
    }

    public void completeUserDeletion() {
        final List<Long> userIds = findWithdrawalTargetsUseCase.execute();
        int completed = 0;
        int skipped = 0;
        int failed = 0;
        for (final Long userId : userIds) {
            try {
                final Optional<List<UserProvider>> unlinkProviders = findWithdrawalUnlinkProvidersUseCase.execute(userId);
                if (unlinkProviders.isEmpty()) {
                    skipped++;
                    continue;
                }
                // 복구 기간이 끝나 되돌릴 수 없으므로 AI 프로필 삭제를 먼저 예약하고, AI 장애는 예약 작업이 회원 ID로 재시도합니다.
                requestProfileSyncUseCase.execute(userId);
                unlinkWithdrawalProvidersUseCase.execute(unlinkProviders.orElseThrow());
                deleteUserOnboardingDataUseCase.execute(userId);
                deleteAllLibraryBooksUseCase.execute(userId);
                if (completeUserDeletionUseCase.execute(userId)) {
                    completed++;
                } else {
                    skipped++;
                }
            } catch (final RuntimeException exception) {
                failed++;
                log.atError().addKeyValue("userId", userId).log("[USER] 회원 최종 탈퇴에 실패했습니다. 다음 실행에서 재시도합니다. userId={}", userId, exception);
            }
        }
        log.info("[USER] 회원 최종 탈퇴를 실행했습니다. target={}, completed={}, skipped={}, failed={}", userIds.size(), completed, skipped, failed);
    }
}
