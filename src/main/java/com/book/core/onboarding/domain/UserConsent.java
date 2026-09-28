package com.book.core.onboarding.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "user_consents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Accessors(fluent = true)
public class UserConsent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 50)
    private ConsentType consentType;

    @Column(name = "policy_version", nullable = false, length = 50)
    private String policyVersion;

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    public UserConsent(final Long id, final Long userId, final ConsentType consentType, final String policyVersion,
        final LocalDateTime agreedAt, final LocalDateTime withdrawnAt) {
        this.id = id;
        this.userId = userId;
        this.consentType = consentType;
        this.policyVersion = policyVersion;
        this.agreedAt = agreedAt;
        this.withdrawnAt = withdrawnAt;
    }

    public static UserConsent create(final Long userId, final ConsentType consentType, final String policyVersion,
        final LocalDateTime agreedAt) {
        return new UserConsent(null, userId, consentType, policyVersion, agreedAt, null);
    }

    public boolean isActive() {
        return withdrawnAt == null;
    }

    public void withdraw(final LocalDateTime withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }
}
