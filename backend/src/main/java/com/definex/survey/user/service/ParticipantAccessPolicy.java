package com.definex.survey.user.service;

import com.definex.survey.exception.UserAccessException;
import com.definex.survey.survey.dto.InvitationContext;
import com.definex.survey.survey.entity.InvitationStatus;
import com.definex.survey.survey.entity.SurveyStatus;
import java.time.Instant;
import org.springframework.http.HttpStatus;

/**
 * Katılımcının ankete erişim kuralları. Login'de ve her {@code /user/**} isteğinde aynı şekilde uygulanır; JWT
 * geçerli olsa bile durum DB'den yeniden okunur. Süre kontrolü durum yerine {@code endsAt} ile yapılır, çünkü
 * süre dolumu zamanlayıcısı dakikada bir çalışır (AS-03).
 */
final class ParticipantAccessPolicy {

    private ParticipantAccessPolicy() {
    }

    static void check(InvitationContext invitation, Instant now) {
        String title = invitation.surveyTitle();
        if (invitation.invitationStatus() == InvitationStatus.CANCELLED) {
            throw invalidInvitation(title);
        }
        if (invitation.invitationStatus() == InvitationStatus.COMPLETED) {
            throw new UserAccessException(HttpStatus.CONFLICT, "ALREADY_RESPONDED", "Bu anketi zaten yanıtladınız", title);
        }
        boolean expired = invitation.surveyStatus() == SurveyStatus.EXPIRED
                || invitation.surveyEndsAt() == null || !invitation.surveyEndsAt().isAfter(now);
        if (expired) {
            throw new UserAccessException(HttpStatus.GONE, "SURVEY_EXPIRED", "Bu anketin süresi dolmuştur", title);
        }
        if (invitation.surveyStatus() != SurveyStatus.PUBLISHED) {
            throw new UserAccessException(HttpStatus.GONE, "SURVEY_CLOSED", "Bu anket artık yanıt kabul etmiyor", title);
        }
    }

    /** Token bulunamadı veya davet iptal: anket başlığı yalnızca davet biliniyorsa döner. */
    static UserAccessException invalidInvitation(String surveyTitle) {
        return new UserAccessException(HttpStatus.UNAUTHORIZED, "INVALID_INVITATION", "Bu davet linki geçersiz",
                surveyTitle);
    }
}
