package com.book.core.notification.application.usecase;

import com.book.core.notification.application.command.GetNotificationsCommand;
import com.book.core.notification.application.port.NotificationRepositoryPort;
import com.book.core.notification.application.result.GetNotificationsResult;
import com.book.core.notification.application.result.NotificationItemResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetNotificationsUseCase {
    private final NotificationRepositoryPort notificationRepository;

    /** 최근 20개 목록과 전체 안 읽은 개수를 동일한 조회 트랜잭션에서 반환합니다. */
    @Transactional(readOnly = true)
    public GetNotificationsResult execute(final GetNotificationsCommand command) {
        final var notifications = notificationRepository.findRecentByUserId(command.userId()).stream()
            .map((final var item) -> new NotificationItemResult(item.id(), item.title(), item.body(), item.createdAt(), item.readAt()))
            .toList();
        return new GetNotificationsResult(notifications, notificationRepository.countUnreadByUserId(command.userId()));
    }
}
