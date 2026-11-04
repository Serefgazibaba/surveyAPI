package com.definex.survey.user.service;

import com.definex.survey.exception.ApiException;
import com.definex.survey.exception.FieldErrorItem;
import com.definex.survey.exception.NotFoundException;
import com.definex.survey.survey.dto.InvitationContext;
import com.definex.survey.survey.dto.ParticipantSurveyForm;
import com.definex.survey.survey.entity.ResponseMode;
import com.definex.survey.survey.service.ParticipantInvitationService;
import com.definex.survey.user.dto.AnswerInput;
import com.definex.survey.user.dto.UserDraftRequest;
import com.definex.survey.user.dto.UserDraftResponse;
import com.definex.survey.user.dto.UserSubmitRequest;
import com.definex.survey.user.dto.UserSubmitResponse;
import com.definex.survey.user.entity.Response;
import com.definex.survey.user.entity.ResponseDraft;
import com.definex.survey.user.repository.ResponseDraftRepository;
import com.definex.survey.user.repository.ResponseRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Katılımcı yanıtları: ara kayıt (FR-63) ve gönderim (FR-64). Erişim kuralları her istekte yeniden uygulanır.
 *
 * <p>Anonim modda yanıt davete bağlanmaz ve gönderim zamanı saat başına yuvarlanır; ara kayıt silinir (BR-06).
 */
@Service
public class UserResponseService {

    static final String THANK_YOU = "Cevaplarınız başarıyla gönderildi. Katılımınız için teşekkür ederiz.";

    private final ParticipantInvitationService invitations;
    private final ResponseDraftRepository draftRepository;
    private final ResponseRepository responseRepository;
    private final Clock clock;

    public UserResponseService(ParticipantInvitationService invitations, ResponseDraftRepository draftRepository,
                               ResponseRepository responseRepository, Clock clock) {
        this.invitations = invitations;
        this.draftRepository = draftRepository;
        this.responseRepository = responseRepository;
        this.clock = clock;
    }

    @Transactional
    public UserDraftResponse saveDraft(ParticipantPrincipal principal, UUID surveyId, UserDraftRequest request) {
        Instant now = clock.instant();
        requireOwnSurvey(principal, surveyId);
        InvitationContext invitation = invitations.findById(principal.invitationId())
                .orElseThrow(() -> ParticipantAccessPolicy.invalidInvitation(null));
        ParticipantAccessPolicy.check(invitation, now);

        ParticipantSurveyForm form = invitations.getForm(surveyId);
        List<FieldErrorItem> invalid = AnswerValidator.validate(form, request.answers());
        if (!invalid.isEmpty()) {
            throw invalidAnswers(invalid);
        }

        ResponseDraft draft = draftRepository.findByInvitationId(invitation.invitationId())
                .orElseGet(() -> new ResponseDraft(invitation.invitationId(), surveyId));
        draft.replace(request.currentQuestionId(), request.answers());
        draft = draftRepository.saveAndFlush(draft);
        return new UserDraftResponse(draft.getUpdatedAt(), (int) AnswerValidator.answeredCount(request.answers()),
                form.questions().size());
    }

    @Transactional(readOnly = true)
    public Optional<ResponseDraft> findDraft(UUID invitationId) {
        return draftRepository.findByInvitationId(invitationId);
    }

    /** Tek transaction: davet kilitlenir, yanıt yazılır, ara kayıt silinir, davet tamamlanır. */
    @Transactional
    public UserSubmitResponse submit(ParticipantPrincipal principal, UUID surveyId, UserSubmitRequest request) {
        Instant now = clock.instant();
        requireOwnSurvey(principal, surveyId);
        // Kilit: aynı anda gelen ikinci gönderim burada bekler, ardından COMPLETED görüp 409 alır.
        InvitationContext invitation = invitations.lockForSubmit(principal.invitationId())
                .orElseThrow(() -> ParticipantAccessPolicy.invalidInvitation(null));
        ParticipantAccessPolicy.check(invitation, now);

        ParticipantSurveyForm form = invitations.getForm(surveyId);
        List<AnswerInput> answers = request.answers();
        List<FieldErrorItem> invalid = AnswerValidator.validate(form, answers);
        if (!invalid.isEmpty()) {
            throw invalidAnswers(invalid);
        }
        List<FieldErrorItem> missing = AnswerValidator.missingRequired(form, answers);
        if (!missing.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REQUIRED_ANSWERS_MISSING",
                    "Lütfen zorunlu soruları yanıtlayın", missing);
        }

        boolean anonymous = form.responseMode() == ResponseMode.ANONYMOUS;
        Optional<ResponseDraft> draft = draftRepository.findByInvitationId(invitation.invitationId());
        Integer duration = draft.map(d -> (int) Math.max(0, Duration.between(d.getCreatedAt(), now).toSeconds()))
                .orElse(null);
        // Anonim modda davetle bağ tutulmaz ve zaman yuvarlanır: davetin tamamlanma zamanıyla eşleştirilemez.
        Response response = new Response(surveyId, anonymous ? null : invitation.invitationId(),
                anonymous ? now.truncatedTo(ChronoUnit.HOURS) : now, duration);
        for (AnswerInput answer : answers) {
            if (answer.isEmpty()) {
                continue;
            }
            String textValue = answer.text() != null && !answer.text().isBlank() ? answer.text()
                    : answer.otherText() != null && !answer.otherText().isBlank() ? answer.otherText() : null;
            response.addAnswer(answer.questionId(), answer.numericValue(), textValue, answer.optionIds());
        }
        responseRepository.save(response);

        draft.ifPresent(draftRepository::delete);
        invitations.markCompleted(invitation.invitationId(), now);
        return new UserSubmitResponse(now, THANK_YOU);
    }

    private static void requireOwnSurvey(ParticipantPrincipal principal, UUID surveyId) {
        if (!principal.surveyId().equals(surveyId)) {
            throw new NotFoundException("SURVEY_NOT_FOUND", "Anket bulunamadı");
        }
    }

    private static ApiException invalidAnswers(List<FieldErrorItem> errors) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_ANSWERS", "Bazı cevaplar geçersiz", errors);
    }
}
