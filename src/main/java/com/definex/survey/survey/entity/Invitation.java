package com.definex.survey.survey.entity;

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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

/**
 * Katılımcının ankete daveti. Davet linkindeki token'ın yalnızca SHA-256 hash'i saklanır (NFR-02).
 * Geçerlilik ayrı tutulmaz; anketin {@code endsAt} değerinden hesaplanır (FR-51).
 * Kişisel bilgi (e-posta, ad) tutulmaz; {@code userId} ile auth'un users tablosundan okunur.
 */
@Entity
@Table(name = "invitations",
        uniqueConstraints = @UniqueConstraint(name = "uk_invitations_survey_user", columnNames = {"survey_id", "user_id"}),
        indexes = @Index(name = "ix_invitations_user_id", columnList = "user_id"))
public class Invitation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvitationStatus status;

    @Column(nullable = false)
    private Instant invitedAt;

    private Instant completedAt;

    protected Invitation() {
    }

    public Invitation(Survey survey, UUID userId, String tokenHash, Instant invitedAt) {
        this.survey = survey;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.status = InvitationStatus.PENDING;
        this.invitedAt = invitedAt;
    }

    public Survey getSurvey() {
        return survey;
    }

    public UUID getUserId() {
        return userId;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public Instant getInvitedAt() {
        return invitedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
