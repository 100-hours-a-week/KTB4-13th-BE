package com.book.core.cancel.api.converter;

import com.book.core.cancel.api.request.CancelRequest;
import com.book.core.cancel.application.command.CancelCommand;
import com.book.core.cancel.domain.CancelAction;
import org.springframework.stereotype.Component;

@Component
public class CancelCommandConverter {

    public CancelCommand toCancelCommand(final Long userId, final CancelRequest request) {
        return new CancelCommand(userId, new CancelAction(request.orderKey()));
    }

}
