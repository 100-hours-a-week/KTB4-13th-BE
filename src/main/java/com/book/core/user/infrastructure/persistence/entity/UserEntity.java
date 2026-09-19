package com.book.core.user.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected UserEntity() {}

    public UserEntity(final Long id, final String nickname, final LocalDateTime deletedAt) {
        this.id = id;
        this.nickname = nickname;
        this.deletedAt = deletedAt;
    }

    public Long id() {
        return id;
    }

    public String nickname() {
        return nickname;
    }

    public LocalDateTime deletedAt() {
        return deletedAt;
    }
}
