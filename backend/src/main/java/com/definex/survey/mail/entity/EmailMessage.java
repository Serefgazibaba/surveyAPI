package com.definex.survey.mail.entity;

import com.definex.survey.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Gönderilecek veya gönderilmiş e-posta. Gövde davet linkini (token) içerdiği için yalnızca gönderim tamamlanana kadar
 * saklanır; {@code SENT} veya {@code FAILED} olunca silinir. Yeniden gönderimde gövde baştan oluşturulur.
 */
@Entity
@Table(name = "email_messages", indexes = {
        @Index(name = "ix_email_messages_owner_status", columnList = "owner_id, status"),
        @Index(name = "ix_email_messages_survey_id", columnList = "survey_id"),
        @Index(name = "ix_email_messages_invitation_id", columnList = "invitation_id")
})
public class EmailMessage extends BaseEntity {

    private static final int MAX_ERROR_LENGTH = 1000;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmailType type;

    /** Maili tetikleyen admin; listeleme ve sahiplik kontrolü için. */
    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "survey_id")
    private UUID surveyId;

    @Column(name = "invitation_id")
    private UUID invitationId;

    @Column(nullable = false, length = 254)
    private String recipientEmail;

    @Column(length = 150)
    private String recipientName;

    @Column(nullable = false, length = 300)
    private String subject;

    @Column(columnDefinition = "text")
    private String bodyHtml;

    @Column(columnDefinition = "text")
    private String bodyText;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailStatus status;

    @Column(nullable = false)
    private int attemptCount;

    @Column(length = MAX_ERROR_LENGTH)
    private String lastError;

    private Instant lastAttemptAt;

    private Instant sentAt;

    protected EmailMessage() {
    }

    public EmailMessage(EmailType type, UUID ownerId, UUID surveyId, UUID invitationId, String recipientEmail,
                        String recipientName, String subject, String bodyHtml, String bodyText) {
        this.type = type;
        this.ownerId = ownerId;
        this.surveyId = surveyId;
        this.invitationId = invitationId;
        this.recipientEmail = recipientEmail;
        this.recipientName = recipientName;
        this.subject = subject;
        this.bodyHtml = bodyHtml;
        this.bodyText = bodyText;
        this.status = EmailStatus.QUEUED;
    }

    public void markSent(Instant now) {
        this.status = EmailStatus.SENT;
        this.sentAt = now;
        this.lastError = null;
        clearBody();
    }

    public void markFailed(String error) {
        this.status = EmailStatus.FAILED;
        this.lastError = error == null ? null
                : error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error;
        clearBody();
    }

    /** Yeniden gönderim için yeni içerikle kuyruğa alır. */
    public void requeue(String subject, String bodyHtml, String bodyText) {
        this.subject = subject;
        this.bodyHtml = bodyHtml;
        this.bodyText = bodyText;
        this.status = EmailStatus.QUEUED;
    }

    /** {@code SENDING} durumunda {@code stuckAfter}'dan uzun süredir bekliyor mu (gönderim sırasında uygulama kapanmış olabilir)? */
    public boolean isStuck(Instant now, Duration stuckAfter) {
        return status == EmailStatus.SENDING && getUpdatedAt() != null && getUpdatedAt().isBefore(now.minus(stuckAfter));
    }

    private void clearBody() {
        this.bodyHtml = null;
        this.bodyText = null;
    }

    public EmailType getType() {
        return type;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public UUID getSurveyId() {
        return surveyId;
    }

    public UUID getInvitationId() {
        return invitationId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getSubject() {
        return subject;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public String getBodyText() {
        return bodyText;
    }

    public EmailStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getLastAttemptAt() {
        return lastAttemptAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
