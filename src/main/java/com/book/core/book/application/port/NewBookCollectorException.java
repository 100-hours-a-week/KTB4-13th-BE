package com.book.core.book.application.port;

import java.util.Optional;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class NewBookCollectorException extends RuntimeException {
    private static final Pattern SAFE_CODE = Pattern.compile("[A-Za-z0-9_-]{1,30}");
    private final NewBookCollectorFailureReason reason;
    private final String externalCode;

    public NewBookCollectorException(final NewBookCollectorFailureReason reason) {
        this(reason, null);
    }

    public NewBookCollectorException(final NewBookCollectorFailureReason reason, final String externalCode) {
        super(reason.name());
        this.reason = reason;
        if (externalCode != null && SAFE_CODE.matcher(externalCode).matches()) {
            this.externalCode = externalCode;
        } else {
            this.externalCode = null;
        }
    }

    public String summary() {
        return Optional.ofNullable(externalCode).map((final var code) -> reason.description() + " (외부 코드: " + code + ")")
            .orElse(reason.description());
    }
}
