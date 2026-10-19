package com.book.core.point.application.port;

import com.book.core.point.domain.PointBalance;

import java.util.Optional;

public interface PointBalanceRepositoryPort {
    Optional<PointBalance> findByUserId(final Long userId);

    Optional<PointBalance> findByUserIdForUpdate(final Long userId);

    /** 회원의 잔액 행이 없을 때만 0원으로 만들고, 동시 생성은 UNIQUE(user_id) 충돌을 무시해 한 행으로 수렴시킵니다. */
    void createIfAbsent(final Long userId);

    void save(final PointBalance balance);
}
