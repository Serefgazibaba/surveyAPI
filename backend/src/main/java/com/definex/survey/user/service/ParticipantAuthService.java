package com.definex.survey.user.service;

import com.definex.survey.security.JwtTokenService;
import com.definex.survey.security.JwtTokenService.AccessToken;
import com.definex.survey.security.Roles;
import com.definex.survey.survey.dto.InvitationContext;
import com.definex.survey.survey.service.ParticipantInvitationService;
import com.definex.survey.user.dto.UserLoginRequest;
import com.definex.survey.user.dto.UserLoginResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Katılımcı girişi: davet linkindeki token, ankete bağlı bir katılımcı JWT'sine çevrilir. Şifre yoktur; davet token'ı
 * kimlik yerine geçer. Refresh yoktur; token anketin bitiş tarihine kadar geçerlidir.
 */
@Service
public class ParticipantAuthService {

    private final ParticipantInvitationService invitations;
    private final JwtTokenService jwtTokenService;
    private final Clock clock;

    public ParticipantAuthService(ParticipantInvitationService invitations, JwtTokenService jwtTokenService,
                                  Clock clock) {
        this.invitations = invitations;
        this.jwtTokenService = jwtTokenService;
        this.clock = clock;
    }

    @Transactional
    public UserLoginResponse login(UserLoginRequest request) {
        Instant now = clock.instant();
        InvitationContext invitation = invitations.findByToken(request.invitationToken())
                .orElseThrow(() -> ParticipantAccessPolicy.invalidInvitation(null));
        ParticipantAccessPolicy.check(invitation, now);

        InvitationContext started = invitations.markStarted(invitation.invitationId());
        AccessToken token = jwtTokenService.issueAccessToken(started.userId().toString(), List.of(Roles.USER),
                Map.of(ParticipantPrincipal.INVITATION_CLAIM, started.invitationId().toString(),
                        ParticipantPrincipal.SURVEY_CLAIM, started.surveyId().toString()),
                started.surveyEndsAt());
        return new UserLoginResponse(token.value(), UserLoginResponse.BEARER, token.expiresInSeconds(),
                token.expiresAt(), started.surveyId(), started.surveyTitle(), started.invitationStatus());
    }
}
