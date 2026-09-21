package com.book.core.user.infrastructure.persistence.entity;

import com.book.core.user.domain.ProviderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_providers")
public class UserProviderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 50)
    private ProviderType providerType;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "provider_email", length = 254)
    private String providerEmail;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    protected UserProviderEntity() {}

    public UserProviderEntity(
            final Long id,
            final Long userId,
            final ProviderType providerType,
            final String providerUserId,
            final String providerEmail,
            final LocalDateTime deletedAt) {
        this.id = id;
        this.userId = userId;
        this.providerType = providerType;
        this.providerUserId = providerUserId;
        this.providerEmail = providerEmail;
        this.deletedAt = deletedAt;
    }

    public Long id() {
        return id;
    }

    public Long userId() {
        return userId;
    }

    public ProviderType providerType() {
        return providerType;
    }

    public String providerUserId() {
        return providerUserId;
    }

    public String providerEmail() {
        return providerEmail;
    }

    public LocalDateTime deletedAt() {
        return deletedAt;
    }
}
