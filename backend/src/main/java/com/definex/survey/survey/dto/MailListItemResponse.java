package com.definex.survey.survey.dto;

import com.definex.survey.mail.entity.EmailStatus;
import com.definex.survey.survey.entity.InvitationStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * @param stuck      mail uzun süredir {@code SENDING} durumunda (gönderim sırasında uygulama kapanmış olabilir)
 * @param resendable admin bu maili yeniden gönderebilir; ön yüz seçim kutusunu buna göre açar
 */
public record MailListItemResponse(
        UUID emailId,
        UUID invitationId,
        UUID surveyId,
        String surveyTitle,
        String recipientName,
        String recipientEmail,
        EmailStatus emailStatus,
        InvitationStatus invitationStatus,
        int attemptCount,
        String lastError,
        Instant lastAttemptAt,
        Instant sentAt,
        boolean stuck,
        boolean resendable) {
}
