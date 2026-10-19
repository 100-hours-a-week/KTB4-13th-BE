package com.book.core.memory.application.command;

import com.book.core.memory.domain.UserMemoryType;
import java.util.List;

public record UserMemoryExtraction(UserMemoryType type, String value, int strength, List<String> categories, String keyword,
    List<Float> vector, int dim) {
}
