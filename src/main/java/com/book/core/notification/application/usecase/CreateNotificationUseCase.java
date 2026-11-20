package com.book.core.notification.application.usecase;

import com.book.core.notification.application.command.CreateNotificationCommand;
import com.book.core.notification.application.event.NotificationCreatedEvent;
import com.book.core.notification.application.port.NotificationRepositoryPort;
import com.book.core.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateNotificationUseCase {
    private final NotificationRepositoryPort notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 내부 업무 기능이 수신자와 내용을 지정해 알림을 저장합니다. */
    @Transactional
    public Long execute(final CreateNotificationCommand command) {
        final Long id = notificationRepository.save(new Notification(command.userId(), command.title(), command.body())).id();
        eventPublisher.publishEvent(new NotificationCreatedEvent(command.userId(), id));
        return id;
    }
}
