package com.book.core.category.domain;

import com.book.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
public class Category extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(nullable = false, length = 255)
    private String path;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public Category(final Long id, final String name, final String path, final LocalDateTime deletedAt) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.deletedAt = deletedAt;
    }

    public boolean isActive() {
        return deletedAt == null;
    }
}
