package com.book.core.cancel.api.converter;

import com.book.core.cancel.api.request.CancelRequest;
import com.book.core.cancel.api.request.PartialCancelRequest;
import com.book.core.cancel.application.command.CancelCommand;
import com.book.core.cancel.application.command.PartialCancelCommand;
import com.book.core.cancel.domain.CancelAction;
import com.book.core.cancel.domain.PartialCancelAction;
import org.springframework.stereotype.Component;

@Component
public class CancelCommandConverter {

    public CancelCommand toCancelCommand(final Long userId, final String requestKey, final CancelRequest request) {
        return new CancelCommand(userId, new CancelAction(request.orderKey()), requestKey);
    }

    public PartialCancelCommand toPartialCancelCommand(final Long userId, final String requestKey, final PartialCancelRequest request) {
        return new PartialCancelCommand(userId, new PartialCancelAction(request.orderKey(), request.orderItemId(), request.quantity()),
            requestKey);
    }
}
