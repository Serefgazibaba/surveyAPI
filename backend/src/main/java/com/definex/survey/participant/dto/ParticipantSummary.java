package com.definex.survey.participant.dto;

import java.util.UUID;

/** @param userId kayıtlı kullanıcıysa users.id; dış katılımcıda {@code null} */
public record ParticipantSummary(UUID id, String fullName, String email, UUID userId) {
}
