package com.definex.survey.survey.dto;

import com.definex.survey.survey.entity.InvitationStatus;
import com.definex.survey.survey.entity.SurveyStatus;
import java.time.Instant;
import java.util.UUID;

/** Katılımcı erişim kontrolü için davet ve anket özeti (survey modülünün dışa açtığı salt okunur görünüm). */
public record InvitationContext(
        UUID invitationId,
        UUID participantId,
        UUID surveyId,
        String surveyTitle,
        SurveyStatus surveyStatus,
        Instant surveyEndsAt,
        InvitationStatus invitationStatus) {
}
