package com.definex.survey.survey.repository;

import com.definex.survey.mail.entity.EmailStatus;
import com.definex.survey.survey.entity.InvitationStatus;
import com.definex.survey.survey.entity.SurveyStatus;
import java.time.Instant;
import java.util.UUID;

/** Davet maili listesinin bir satırı: mail + davet + anket bilgisi. */
public record InvitationMailRow(
        UUID emailId,
        UUID invitationId,
        UUID surveyId,
        String surveyTitle,
        SurveyStatus surveyStatus,
        Instant surveyEndsAt,
        String recipientName,
        String recipientEmail,
        EmailStatus emailStatus,
        InvitationStatus invitationStatus,
        int attemptCount,
        String lastError,
        Instant lastAttemptAt,
        Instant sentAt,
        Instant emailUpdatedAt,
        Instant emailCreatedAt) {
}
