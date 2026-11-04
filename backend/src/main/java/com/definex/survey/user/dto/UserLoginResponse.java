package com.definex.survey.user.dto;

import com.definex.survey.survey.entity.InvitationStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Katılımcı oturumu. Refresh token yoktur; token anketin bitiş tarihine kadar geçerlidir. Token kaybolursa katılımcı
 * davet linkine tekrar tıklayarak giriş yapar.
 *
 * @param expiresIn token'ın kalan ömrü (saniye)
 * @param surveyId  ön yüzün {@code GET /user/surveys/{surveyId}} için kullanacağı anket
 */
public record UserLoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant expiresAt,
        UUID surveyId,
        String surveyTitle,
        InvitationStatus invitationStatus) {

    public static final String BEARER = "Bearer";
}
