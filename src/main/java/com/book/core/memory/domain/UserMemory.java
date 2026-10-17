package com.book.core.memory.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "user_memories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class UserMemory extends BaseEntity {
    private static final int MAX_STRENGTH = 3;
    private static final int MAX_CATEGORIES = 3;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private UserMemoryType type;

    @Column(name = "value", nullable = false, length = 200)
    private String value;

    @Column(name = "strength", nullable = false, columnDefinition = "TINYINT")
    private int strength;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "categories", nullable = false)
    private List<String> categories;

    @Column(name = "keyword", nullable = false, length = 100)
    private String keyword;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vector", nullable = false)
    private List<Float> vector;

    @Column(name = "dim", nullable = false, columnDefinition = "SMALLINT")
    private int dim;

    @Column(name = "conversation_id", nullable = false, length = 200)
    private String conversationId;

    public UserMemory(final Long userId, final UserMemoryType type, final String value, final int strength, final List<String> categories,
        final String keyword, final List<Float> vector, final int dim, final String conversationId) {
        validate(userId, type, value, strength, categories, keyword, vector, dim, conversationId);
        this.userId = userId;
        this.type = type;
        this.value = value;
        this.strength = strength;
        this.categories = List.copyOf(categories);
        this.keyword = keyword;
        this.vector = List.copyOf(vector);
        this.dim = dim;
        this.conversationId = conversationId;
    }

    public boolean hasSameKeywordAs(final UserMemory other) {
        return type == other.type && normalizedKeyword().equals(other.normalizedKeyword());
    }

    public String normalizedKeyword() {
        return keyword.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static void validate(final Long userId, final UserMemoryType type, final String value, final int strength,
        final List<String> categories, final String keyword, final List<Float> vector, final int dim, final String conversationId) {
        if (userId == null || type == null || isBlank(value) || isBlank(keyword) || isBlank(conversationId)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (strength == 0 || Math.abs(strength) > MAX_STRENGTH) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (categories == null || categories.size() > MAX_CATEGORIES) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (dim <= 0 || vector == null || vector.size() != dim) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    private static boolean isBlank(final String text) {
        return text == null || text.isBlank();
    }
}
