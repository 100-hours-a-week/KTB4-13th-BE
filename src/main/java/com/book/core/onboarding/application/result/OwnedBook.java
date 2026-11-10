package com.book.core.onboarding.application.result;

import java.time.LocalDateTime;

public record OwnedBook(String isbn, LocalDateTime acquiredAt, boolean purchased) {
}
