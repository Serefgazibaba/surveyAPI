package com.definex.survey.survey.dto;

import com.definex.survey.mail.entity.EmailStatus;
import java.util.UUID;

/** Oluşturulan davet ve mailinin durumu. Davet linki yanıtta dönmez; yalnızca maille gider. */
public record InvitationSummaryResponse(UUID userId, String email, String fullName, EmailStatus emailStatus) {
}
