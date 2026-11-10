package com.book.core.onboarding.application.usecase;

import com.book.common.annotation.UseCase;
import com.book.core.onboarding.application.port.ProfileSyncJobRepositoryPort;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 취향 프로필을 현재 회원·동의 상태에 맞추는 작업을 예약합니다. 작업자는 처리 시점의 상태로 갱신·삭제·생략을 정하므로, 프로필을 지워야 하는 변경은 예약이 처리되기
 * 전에 그 상태가 확정돼 있어야 합니다. 최종 탈퇴는 복구 기간이 끝나 되돌릴 수 없는 시점에 예약하고, 동의 철회는 철회를 저장하는 트랜잭션 안에서 예약합니다. 새 예약은
 * 대기 중인 이전 작업을 대체합니다.
 */
@UseCase
@RequiredArgsConstructor
public class RequestProfileSyncUseCase {
    private final ProfileSyncJobRepositoryPort profileJobs;
    private final Clock clock;

    @Transactional
    public void execute(final Long userId) {
        profileJobs.enqueue(userId, clock.instant());
    }
}
