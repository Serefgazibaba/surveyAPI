package com.definex.survey.survey.dto;

import com.definex.survey.mail.entity.EmailStatus;
import java.util.UUID;

/**
 * Oluşturulan davet ve mailinin durumu. Davet linki yanıtta dönmez; yalnızca maille gider.
 *
 * @param userId kişi uygulamada kayıtlı bir kullanıcıysa dolu; dış katılımcıda yanıtta yer almaz
 */
public record InvitationSummaryResponse(UUID participantId, UUID userId, String email, String fullName,
                                        EmailStatus emailStatus) {
}
