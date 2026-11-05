package com.definex.survey.survey.service;

import com.definex.survey.auth.dto.UserSummaryResponse;
import com.definex.survey.auth.service.UserDirectoryService;
import com.definex.survey.config.FrontendProperties;
import com.definex.survey.exception.ApiException;
import com.definex.survey.exception.FieldErrorItem;
import com.definex.survey.exception.NotFoundException;
import com.definex.survey.security.SecureTokenGenerator;
import com.definex.survey.mail.entity.EmailType;
import com.definex.survey.mail.service.EmailRequest;
import com.definex.survey.mail.service.MailService;
import com.definex.survey.mail.service.MailService.QueuedEmail;
import com.definex.survey.participant.dto.ParticipantSummary;
import com.definex.survey.participant.service.ParticipantDirectoryService;
import com.definex.survey.survey.dto.InvitationSummaryResponse;
import com.definex.survey.survey.dto.PublishSurveyRequest;
import com.definex.survey.survey.dto.PublishSurveyResponse;
import com.definex.survey.survey.entity.Invitation;
import com.definex.survey.survey.entity.Survey;
import com.definex.survey.survey.entity.SurveyStatus;
import com.definex.survey.survey.repository.InvitationRepository;
import com.definex.survey.survey.repository.SurveyRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Anketi yayınlar: seçilen katılımcılar için davet ve davet maili oluşturur, anketi {@code DRAFT} → {@code PUBLISHED}
 * yapar. Mailler aynı transaction'da kaydedilir ve commit sonrası arka planda gönderilir. Davet linki yalnızca maille
 * gider; yanıtta dönmez.
 */
@Service
public class SurveyPublishService {

    private static final Logger log = LoggerFactory.getLogger(SurveyPublishService.class);
    private static final int MAX_PARTICIPANTS = 1000;

    private final SurveyRepository surveyRepository;
    private final InvitationRepository invitationRepository;
    private final UserDirectoryService userDirectory;
    private final ParticipantDirectoryService participantDirectory;
    private final SecureTokenGenerator tokenGenerator;
    private final FrontendProperties frontend;
    private final MailService mailService;
    private final InvitationEmailComposer emailComposer;
    private final Clock clock;

    public SurveyPublishService(SurveyRepository surveyRepository, InvitationRepository invitationRepository,
                                UserDirectoryService userDirectory, ParticipantDirectoryService participantDirectory,
                                SecureTokenGenerator tokenGenerator,
                                FrontendProperties frontend, MailService mailService,
                                InvitationEmailComposer emailComposer, Clock clock) {
        this.surveyRepository = surveyRepository;
        this.invitationRepository = invitationRepository;
        this.userDirectory = userDirectory;
        this.participantDirectory = participantDirectory;
        this.tokenGenerator = tokenGenerator;
        this.frontend = frontend;
        this.mailService = mailService;
        this.emailComposer = emailComposer;
        this.clock = clock;
    }

    @Transactional
    public PublishSurveyResponse publish(UUID ownerId, PublishSurveyRequest request) {
        Instant now = clock.instant();
        Survey survey = surveyRepository.findForUpdateByIdAndOwnerId(request.surveyId(), ownerId)
                .orElseThrow(() -> new NotFoundException("SURVEY_NOT_FOUND", "Anket bulunamadı"));
        if (survey.getStatus() != SurveyStatus.DRAFT) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_SURVEY_STATUS", "Yalnızca taslak anket yayınlanabilir");
        }

        List<FieldErrorItem> incomplete = SurveyPublishValidator.validate(survey, now);
        if (!incomplete.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "SURVEY_INCOMPLETE",
                    "Anket yayınlanmaya hazır değil", incomplete);
        }

        if (request.userIdsOrEmpty().isEmpty() && request.participantsOrEmpty().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "İstek doğrulanamadı",
                    List.of(new FieldErrorItem("participants", "NotEmpty", "En az bir katılımcı seçilmelidir")));
        }
        List<UserSummaryResponse> registered = resolveRegistered(request.userIdsOrEmpty());
        List<ParticipantSummary> participants = participantDirectory.resolve(registered, request.participantsOrEmpty());
        if (participants.size() > MAX_PARTICIPANTS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "İstek doğrulanamadı",
                    List.of(new FieldErrorItem("participants", "Size", "En fazla " + MAX_PARTICIPANTS + " katılımcı davet edilebilir")));
        }

        List<InvitationSummaryResponse> invitations = new ArrayList<>();
        for (ParticipantSummary user : participants) {
            String token = tokenGenerator.generate();
            Invitation invitation = invitationRepository.save(
                    new Invitation(survey, user.id(), tokenGenerator.hash(token), now));
            String url = frontend.invitationUrl(token);
            if (frontend.logInvitationLinks()) {
                log.info("Davet linki: survey={} user={} url={}", survey.getId(), user.email(), url);
            }
            // Aynı transaction'da kaydedilir; commit edilince arka planda gönderilir.
            QueuedEmail email = mailService.queue(new EmailRequest(EmailType.SURVEY_INVITATION, ownerId,
                    survey.getId(), invitation.getId(), user.email(), user.fullName(),
                    emailComposer.compose(user.fullName(), survey.getTitle(), survey.getEndsAt(), url)));
            invitations.add(new InvitationSummaryResponse(user.id(), user.userId(), user.email(), user.fullName(),
                    email.status()));
        }
        survey.publish(now);

        return new PublishSurveyResponse(survey.getId(), survey.getStatus(), survey.getPublishedAt(),
                invitations.size(), invitations);
    }

    /** Kayıtlı kullanıcılar: tekrarları atar, sırayı korur; davet edilemeyen id'ler için istek dizisindeki konumla hata döner. */
    private List<UserSummaryResponse> resolveRegistered(List<UUID> requested) {
        if (requested.isEmpty()) {
            return List.of();
        }
        Set<UUID> unique = new LinkedHashSet<>(requested);
        Map<UUID, UserSummaryResponse> found = userDirectory.findInvitableByIds(unique);

        List<FieldErrorItem> errors = new ArrayList<>();
        Set<UUID> reported = new HashSet<>();
        for (int i = 0; i < requested.size(); i++) {
            UUID id = requested.get(i);
            if (!found.containsKey(id) && reported.add(id)) {
                errors.add(new FieldErrorItem("userIds[" + i + "]", "NOT_INVITABLE",
                        "Kullanıcı bulunamadı, katılımcı rolünde değil veya pasif"));
            }
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PARTICIPANTS",
                    "Seçilen katılımcılar geçersiz", errors);
        }
        return unique.stream().map(found::get).toList();
    }
}
