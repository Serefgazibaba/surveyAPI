package com.definex.survey.mail.service;

import com.definex.survey.mail.entity.EmailType;
import java.util.UUID;

/**
 * Kuyruğa alınacak e-posta.
 *
 * @param ownerId      maili tetikleyen admin
 * @param surveyId     ilgili anket (opsiyonel)
 * @param invitationId ilgili davet (opsiyonel)
 */
public record EmailRequest(
        EmailType type,
        UUID ownerId,
        UUID surveyId,
        UUID invitationId,
        String recipientEmail,
        String recipientName,
        EmailContent content) {
}
