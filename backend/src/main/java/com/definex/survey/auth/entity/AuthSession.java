package com.definex.survey.auth.entity;

import com.definex.survey.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

/**
 * Bir login işlemi. Id'si JWT'de {@code sid} claim'i olarak taşınır. Refresh token'ın yalnızca hash'i saklanır;
 * rotation'da geçerli hash {@code previousRefreshTokenHash}'e taşınır (yeniden kullanım tespiti).
 */
@Entity
@Table(name = "auth_sessions", indexes = {
        @Index(name = "ix_auth_sessions_user_id", columnList = "user_id"),
        @Index(name = "ix_auth_sessions_previous_refresh_token_hash", columnList = "previous_refresh_token_hash")
})
public class AuthSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 512)
    private String userAgent;

    @Column(nullable = false, unique = true, length = 64)
    private String refreshTokenHash;

    @Column(name = "previous_refresh_token_hash", length = 64)
    private String previousRefreshTokenHash;

    @Column(nullable = false)
    private Instant lastActivityAt;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private RevokeReason revokeReason;

    protected AuthSession() {
    }

    public AuthSession(User user, String ipAddress, String userAgent, String refreshTokenHash,
                       Instant now, Duration maxLifetime) {
        this.user = user;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.refreshTokenHash = refreshTokenHash;
        this.lastActivityAt = now;
        this.expiresAt = now.plus(maxLifetime);
    }

    /** Geçerli hash'i önceki hash'e taşır, yeni hash'i yazar ve son hareket zamanını günceller. */
    public void rotateRefreshToken(String newRefreshTokenHash, Instant now) {
        this.previousRefreshTokenHash = this.refreshTokenHash;
        this.refreshTokenHash = newRefreshTokenHash;
        this.lastActivityAt = now;
    }

    /** İdempotent; zaten iptal edilmiş oturumun ilk nedeni korunur. */
    public void revoke(RevokeReason reason, Instant now) {
        if (revokedAt == null) {
            this.revokedAt = now;
            this.revokeReason = reason;
        }
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isIdle(Instant now, Duration idleTimeout) {
        return now.isAfter(lastActivityAt.plus(idleTimeout));
    }

    public User getUser() {
        return user;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public RevokeReason getRevokeReason() {
        return revokeReason;
    }
}
