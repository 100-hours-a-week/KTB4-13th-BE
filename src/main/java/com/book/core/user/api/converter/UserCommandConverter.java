package com.book.core.user.api.converter;

import com.book.core.user.api.request.UpdateMyInfoRequest;
import com.book.core.user.application.command.UpdateMyInfoCommand;
import org.springframework.stereotype.Component;

@Component
public class UserCommandConverter {
    public UpdateMyInfoCommand toUpdateMyInfoCommand(final Long userId, final UpdateMyInfoRequest request) {
        return new UpdateMyInfoCommand(userId, request.nickname());
    }
}
