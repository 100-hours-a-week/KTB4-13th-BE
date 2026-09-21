package com.book.core.address.application.command;

public record PatchField<T>(boolean provided, T value) {
    public static <T> PatchField<T> absent() {
        return new PatchField<>(false, null);
    }

    public static <T> PatchField<T> of(final T value) {
        return new PatchField<>(true, value);
    }

    public T valueOrElse(final T currentValue) {
        if (provided) {
            return value;
        }
        return currentValue;
    }
}
