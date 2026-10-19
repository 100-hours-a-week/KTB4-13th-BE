package com.book.core.preference.api.converter;

import com.book.core.memory.application.command.DeleteUserMemoriesCommand;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UserPreferenceCommandConverter {
    public DeleteUserMemoriesCommand toDeleteUserMemoriesCommand(final Long userId, final List<Long> memoryIds) {
        return new DeleteUserMemoriesCommand(userId, memoryIds);
    }
}
