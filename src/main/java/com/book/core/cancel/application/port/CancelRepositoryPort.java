package com.book.core.cancel.application.port;

import com.book.core.cancel.domain.Cancel;

/** 취소 내역 저장과 첫 취소 여부를 확인합니다. */
public interface CancelRepositoryPort {
    Cancel save(final Cancel cancel);

    long countByOrderId(final Long orderId);
}
