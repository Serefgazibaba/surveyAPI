package com.definex.survey.survey.service;

import com.definex.survey.security.SecureTokenGenerator;
import com.definex.survey.survey.dto.InvitationContext;
import com.definex.survey.survey.dto.ParticipantSurveyForm;
import com.definex.survey.survey.entity.Invitation;
import com.definex.survey.survey.entity.Survey;
import com.definex.survey.survey.repository.InvitationRepository;
import com.definex.survey.survey.repository.SurveyRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Katılımcı (user) modülüne açılan davet ve form işlemleri. Survey'in entity ve repository'leri modül dışına
 * çıkmaz; yalnızca bu service'in DTO'ları kullanılır.
 */
@Service
public class ParticipantInvitationService {

    private final InvitationRepository invitationRepository;
    private final SurveyRepository surveyRepository;
    private final SecureTokenGenerator tokenGenerator;

    public ParticipantInvitationService(InvitationRepository invitationRepository, SurveyRepository surveyRepository,
                                        SecureTokenGenerator tokenGenerator) {
        this.invitationRepository = invitationRepository;
        this.surveyRepository = surveyRepository;
        this.tokenGenerator = tokenGenerator;
    }

    /** Davet linkindeki ham token ile; yalnızca hash karşılaştırılır. */
    @Transactional(readOnly = true)
    public Optional<InvitationContext> findByToken(String rawToken) {
        return invitationRepository.findByTokenHash(tokenGenerator.hash(rawToken)).map(ParticipantInvitationService::toContext);
    }

    @Transactional(readOnly = true)
    public Optional<InvitationContext> findById(UUID invitationId) {
        return invitationRepository.findWithSurveyById(invitationId).map(ParticipantInvitationService::toContext);
    }

    /** {@code PENDING} → {@code STARTED}; zaten başlamışsa değişmez. */
    @Transactional
    public InvitationContext markStarted(UUID invitationId) {
        Invitation invitation = invitationRepository.findWithSurveyById(invitationId).orElseThrow();
        invitation.markStarted();
        return toContext(invitation);
    }

    @Transactional(readOnly = true)
    public ParticipantSurveyForm getForm(UUID surveyId) {
        Survey survey = surveyRepository.findById(surveyId).orElseThrow();
        return new ParticipantSurveyForm(survey.getId(), survey.getTitle(), survey.getDescription(),
                survey.getResponseMode(), survey.getEndsAt(),
                survey.getQuestions().stream()
                        .map(q -> new ParticipantSurveyForm.Question(q.getId(), q.getSortOrder() + 1, q.getText(),
                                q.getType(), q.isRequired(), q.getSettings(),
                                q.getOptions().stream()
                                        .map(o -> new ParticipantSurveyForm.Option(o.getId(), o.getLabel()))
                                        .toList()))
                        .toList());
    }

    private static InvitationContext toContext(Invitation invitation) {
        Survey survey = invitation.getSurvey();
        return new InvitationContext(invitation.getId(), invitation.getUserId(), survey.getId(), survey.getTitle(),
                survey.getStatus(), survey.getEndsAt(), invitation.getStatus());
    }
}
