package com.definex.survey.survey.service;

import com.definex.survey.auth.dto.UserSummaryResponse;
import com.definex.survey.auth.service.UserDirectoryService;
import com.definex.survey.config.FrontendProperties;
import com.definex.survey.exception.ApiException;
import com.definex.survey.exception.FieldErrorItem;
import com.definex.survey.exception.NotFoundException;
import com.definex.survey.security.SecureTokenGenerator;
import com.definex.survey.survey.dto.InvitationLinkResponse;
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
 * Anketi yayınlar: seçilen katılımcılar için davet oluşturur ve anketi {@code DRAFT} → {@code PUBLISHED} yapar.
 * Davet e-postası henüz gönderilmez; linkler yanıtta döner (geçici).
 */
@Service
public class SurveyPublishService {

    private static final Logger log = LoggerFactory.getLogger(SurveyPublishService.class);

    private final SurveyRepository surveyRepository;
    private final InvitationRepository invitationRepository;
    private final UserDirectoryService userDirectory;
    private final SecureTokenGenerator tokenGenerator;
    private final FrontendProperties frontend;
    private final Clock clock;

    public SurveyPublishService(SurveyRepository surveyRepository, InvitationRepository invitationRepository,
                                UserDirectoryService userDirectory, SecureTokenGenerator tokenGenerator,
                                FrontendProperties frontend, Clock clock) {
        this.surveyRepository = surveyRepository;
        this.invitationRepository = invitationRepository;
        this.userDirectory = userDirectory;
        this.tokenGenerator = tokenGenerator;
        this.frontend = frontend;
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

        List<UserSummaryResponse> participants = resolveParticipants(request.userIds());

        List<InvitationLinkResponse> links = new ArrayList<>();
        for (UserSummaryResponse user : participants) {
            String token = tokenGenerator.generate();
            invitationRepository.save(new Invitation(survey, user.id(), tokenGenerator.hash(token), now));
            String url = frontend.invitationUrl(token);
            links.add(new InvitationLinkResponse(user.id(), user.email(), user.fullName(), url));
            if (frontend.logInvitationLinks()) {
                log.info("Davet linki: survey={} user={} url={}", survey.getId(), user.email(), url);
            }
        }
        survey.publish(now);

        return new PublishSurveyResponse(survey.getId(), survey.getStatus(), survey.getPublishedAt(),
                links.size(), links);
    }

    /** Tekrarları atar, sırayı korur; davet edilemeyen id'ler için istek dizisindeki konumla hata döner. */
    private List<UserSummaryResponse> resolveParticipants(List<UUID> requested) {
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
