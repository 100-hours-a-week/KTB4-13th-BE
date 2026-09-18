package com.book.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@MappedSuperclass
public abstract class BaseEntity extends BaseTimeEntity {

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
