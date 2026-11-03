package com.definex.survey.auth.entity;

import com.definex.survey.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * (e-posta, IP) başına hatalı giriş sayacı ve kilit (FR-01). Kullanıcı olmayan e-postalar için de tutulur.
 * Sayaç artışı eşzamanlılık için native upsert ile yapılır, bkz. {@code LoginAttemptRepository#recordFailure}.
 */
@Entity
@Table(name = "login_attempts",
        uniqueConstraints = @UniqueConstraint(name = "uk_login_attempts_email_ip", columnNames = {"email", "ip_address"}),
        indexes = @Index(name = "ix_login_attempts_email", columnList = "email"))
public class LoginAttempt extends BaseEntity {

    // Kolon adları açık yazılır: unique constraint ve index tanımları fiziksel adları kullanır.
    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(nullable = false)
    private int failedCount;

    private Instant lockedUntil;

    @Column(nullable = false)
    private Instant lastFailedAt;

    protected LoginAttempt() {
    }

    public String getEmail() {
        return email;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getLastFailedAt() {
        return lastFailedAt;
    }
}
