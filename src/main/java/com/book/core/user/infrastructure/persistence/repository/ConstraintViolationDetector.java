package com.book.core.user.infrastructure.persistence.repository;

import org.hibernate.exception.ConstraintViolationException;

final class ConstraintViolationDetector {
    private ConstraintViolationDetector() {}

    static boolean hasConstraint(final Throwable exception, final String expectedConstraintName) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof final ConstraintViolationException constraintViolation) {
                final String constraintName = constraintViolation.getConstraintName();
                return constraintName != null && constraintName.endsWith(expectedConstraintName);
            }
            cause = cause.getCause();
        }
        return false;
    }
}
