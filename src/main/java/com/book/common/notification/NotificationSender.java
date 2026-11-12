package com.book.common.notification;

/** 완성된 알림 메시지를 전송합니다. */
public interface NotificationSender {
    void send(String message);
}
