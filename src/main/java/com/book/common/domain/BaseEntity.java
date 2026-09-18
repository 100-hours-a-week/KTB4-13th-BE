package com.book.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@MappedSuperclass
public abstract class BaseEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EntityStatus status = EntityStatus.ACTIVE;

    void active() {
        this.status = EntityStatus.ACTIVE;
    }

    boolean isActive() {
        return this.status == EntityStatus.ACTIVE;
    }

    void delete() {
        this.status = EntityStatus.DELETED;
    }

    boolean isDeleted() {
        return this.status == EntityStatus.DELETED;
    }
}
