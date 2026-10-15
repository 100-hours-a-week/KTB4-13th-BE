package com.book.core.library.domain;

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
@Table(name = "user_library_books")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class UserLibraryBook extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    public UserLibraryBook(final Long id, final Long userId, final Long bookId, final LocalDateTime addedAt) {
        super(id);
        this.userId = userId;
        this.bookId = bookId;
        this.addedAt = addedAt;
    }

    public static UserLibraryBook add(final Long userId, final Long bookId, final LocalDateTime addedAt) {
        return new UserLibraryBook(null, userId, bookId, addedAt);
    }

    /** 삭제된 항목이면 복구하고 추가 시각을 갱신합니다. 활성 항목은 변경하지 않습니다. */
    public void restore(final LocalDateTime now) {
        if (isActive()) {
            return;
        }
        active();
        this.addedAt = now;
    }
}
