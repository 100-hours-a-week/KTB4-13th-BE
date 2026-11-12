package com.book.core.book.domain;

import com.book.common.domain.BaseEntity;
import com.book.common.exception.CoreException;
import com.book.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Entity
@Table(name = "books")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class Book extends BaseEntity {
    @Version
    @Column(nullable = false)
    private long version;

    @Column(length = 20)
    private String isbn13;

    @Column(name = "imported_isbn13", length = 13, updatable = false)
    private String importedIsbn13;

    @Column(length = 20)
    private String isbn10;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String publisher;

    @Column(nullable = false)
    private String category;

    @Column(name = "published_at", nullable = false)
    private LocalDate publishedAt;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    public static Book registerNewBook(
        final String isbn13,
        final String title,
        final String author,
        final String description,
        final String publisher,
        final String category,
        final LocalDate publishedAt,
        final String coverImageUrl
    ) {
        final String normalizedIsbn13 = Isbn13.normalize(isbn13).orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));
        if (title == null || title.isBlank() || author == null || author.isBlank() || publisher == null || publisher.isBlank()
            || category == null || category.isBlank() || publishedAt == null) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        final Book book =
            new Book(null, normalizedIsbn13, null, title, author, description, publisher, category, publishedAt, coverImageUrl);
        book.importedIsbn13 = normalizedIsbn13;
        return book;
    }

    public Book(
        final Long id,
        final String isbn13,
        final String isbn10,
        final String title,
        final String author,
        final String description,
        final String publisher,
        final String category,
        final LocalDate publishedAt,
        final String coverImageUrl
    ) {
        super(id);
        this.isbn13 = isbn13;
        this.isbn10 = isbn10;
        this.title = title;
        this.author = author;
        this.description = description;
        this.publisher = publisher;
        this.category = category;
        this.publishedAt = publishedAt;
        this.coverImageUrl = coverImageUrl;
    }
}
