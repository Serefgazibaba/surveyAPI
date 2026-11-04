package com.definex.survey.user.service;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Katılımcı JWT'sinin içeriği: {@code sub} = katılımcı (participants.id), {@code inv} = davet, {@code svy} = anket.
 */
public record ParticipantPrincipal(UUID participantId, UUID invitationId, UUID surveyId) {

    public static final String INVITATION_CLAIM = "inv";
    public static final String SURVEY_CLAIM = "svy";

    public static ParticipantPrincipal from(Jwt jwt) {
        return new ParticipantPrincipal(UUID.fromString(jwt.getSubject()),
                UUID.fromString(jwt.getClaimAsString(INVITATION_CLAIM)),
                UUID.fromString(jwt.getClaimAsString(SURVEY_CLAIM)));
    }
}
