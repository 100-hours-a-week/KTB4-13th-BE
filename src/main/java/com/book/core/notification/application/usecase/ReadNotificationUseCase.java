package com.book.core.notification.application.usecase;

import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import com.book.core.notification.application.command.ReadNotificationCommand;
import com.book.core.notification.application.port.NotificationRepositoryPort;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReadNotificationUseCase {
    private final NotificationRepositoryPort notificationRepository;

    /** 본인 알림을 잠근 뒤 최초 읽음 시각을 기록하고 반복 요청은 성공합니다. */
    @Transactional
    public void execute(final ReadNotificationCommand command) {
        final var notification = notificationRepository.findOwnedActiveForUpdate(command.userId(), command.notificationId())
            .orElseThrow(() -> new CoreException(ErrorCode.NOTIFICATION_NOT_FOUND));
        notification.markRead(LocalDateTime.now());
        notificationRepository.save(notification);
    }
}
