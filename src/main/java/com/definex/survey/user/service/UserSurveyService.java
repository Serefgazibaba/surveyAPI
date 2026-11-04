package com.definex.survey.user.service;

import com.definex.survey.exception.NotFoundException;
import com.definex.survey.survey.dto.InvitationContext;
import com.definex.survey.survey.dto.ParticipantSurveyForm;
import com.definex.survey.survey.entity.ResponseMode;
import com.definex.survey.survey.service.ParticipantInvitationService;
import com.definex.survey.user.dto.UserSurveyResponse;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Katılımcının anket formu. Token yalnızca kendi anketini açar; erişim kuralları her istekte yeniden uygulanır. */
@Service
public class UserSurveyService {

    static final String ANONYMOUS_NOTICE =
            "Yanıtlarınız anonim olarak toplanmaktadır; kimliğinizle ilişkilendirilmez.";

    private final ParticipantInvitationService invitations;
    private final Clock clock;

    public UserSurveyService(ParticipantInvitationService invitations, Clock clock) {
        this.invitations = invitations;
        this.clock = clock;
    }

    public UserSurveyResponse getSurvey(ParticipantPrincipal principal, UUID surveyId) {
        if (!principal.surveyId().equals(surveyId)) {
            throw new NotFoundException("SURVEY_NOT_FOUND", "Anket bulunamadı");
        }
        InvitationContext invitation = invitations.findById(principal.invitationId())
                .orElseThrow(() -> ParticipantAccessPolicy.invalidInvitation(null));
        ParticipantAccessPolicy.check(invitation, clock.instant());

        ParticipantSurveyForm form = invitations.getForm(surveyId);
        return new UserSurveyResponse(form.id(), form.title(), form.description(), form.responseMode(),
                form.responseMode() == ResponseMode.ANONYMOUS ? ANONYMOUS_NOTICE : null,
                form.endsAt(), form.questions().size(),
                form.questions().stream()
                        .map(q -> new UserSurveyResponse.Question(q.id(), q.number(), q.text(), q.type(),
                                q.required(), q.settings(),
                                q.options().stream().map(o -> new UserSurveyResponse.Option(o.id(), o.label())).toList()))
                        .toList(),
                null);
    }
}
