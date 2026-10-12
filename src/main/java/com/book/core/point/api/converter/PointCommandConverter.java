package com.book.core.point.api.converter;

import com.book.core.point.application.command.GetPointCommand;
import org.springframework.stereotype.Component;

@Component
public class PointCommandConverter {
    public GetPointCommand toGetPointCommand(final Long userId) {
        return new GetPointCommand(userId);
    }
}
