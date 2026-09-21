package com.book.core.category.domain;

import com.book.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "book_category")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Category extends BaseEntity {
    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 255)
    private String path;

    public Category(final Long id, final String name, final String path, final LocalDateTime deletedAt) {
        super(id, deletedAt);
        this.name = name;
        this.path = path;
    }

    @Override
    public boolean isActive() {
        return deletedAt() == null;
    }
}
