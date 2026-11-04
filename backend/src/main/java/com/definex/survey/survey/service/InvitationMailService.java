package com.definex.survey.survey.service;

import com.definex.survey.common.dto.PageMeta;
import com.definex.survey.config.FrontendProperties;
import com.definex.survey.config.MailProperties;
import com.definex.survey.exception.ApiException;
import com.definex.survey.exception.FieldErrorItem;
import com.definex.survey.mail.entity.EmailMessage;
import com.definex.survey.mail.entity.EmailStatus;
import com.definex.survey.mail.service.MailService;
import com.definex.survey.mail.service.MailService.QueuedEmail;
import com.definex.survey.security.SecureTokenGenerator;
import com.definex.survey.survey.dto.MailListItemResponse;
import com.definex.survey.survey.dto.MailListResponse;
import com.definex.survey.survey.dto.ResendMailsResponse;
import com.definex.survey.survey.entity.Invitation;
import com.definex.survey.survey.entity.InvitationStatus;
import com.definex.survey.survey.entity.SurveyStatus;
import com.definex.survey.survey.repository.InvitationMailQueryRepository;
import com.definex.survey.survey.repository.InvitationMailQueryRepository.MailFilter;
import com.definex.survey.survey.repository.InvitationMailRow;
import com.definex.survey.survey.repository.InvitationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Davet maillerinin listelenmesi ve yeniden gönderimi. Otomatik yeniden deneme yoktur; admin gönderilemeyen
 * mailleri seçerek yeniden gönderir.
 */
@Service
public class InvitationMailService {

    private static final Logger log = LoggerFactory.getLogger(InvitationMailService.class);

    private final InvitationMailQueryRepository queryRepository;
    private final InvitationRepository invitationRepository;
    private final MailService mailService;
    private final InvitationEmailComposer emailComposer;
    private final SecureTokenGenerator tokenGenerator;
    private final FrontendProperties frontend;
    private final MailProperties mailProperties;
    private final Clock clock;

    public InvitationMailService(InvitationMailQueryRepository queryRepository, InvitationRepository invitationRepository,
                                 MailService mailService, InvitationEmailComposer emailComposer,
                                 SecureTokenGenerator tokenGenerator, FrontendProperties frontend,
                                 MailProperties mailProperties, Clock clock) {
        this.queryRepository = queryRepository;
        this.invitationRepository = invitationRepository;
        this.mailService = mailService;
        this.emailComposer = emailComposer;
        this.tokenGenerator = tokenGenerator;
        this.frontend = frontend;
        this.mailProperties = mailProperties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MailListResponse list(MailFilter filter, int page, int size) {
        Instant now = clock.instant();
        List<MailListItemResponse> items = queryRepository.find(filter, (page - 1) * size, size).stream()
                .map(row -> toItem(row, now))
                .toList();
        long total = queryRepository.count(filter);
        PageMeta meta = PageMeta.from(new PageImpl<>(items, PageRequest.of(page - 1, size), total));
        return new MailListResponse(items, queryRepository.countByEmailStatus(filter), meta);
    }

    /**
     * Seçilen mailleri yeni token'la yeniden kuyruğa alır. Herhangi biri gönderilemez durumdaysa hiçbiri gönderilmez.
     */
    @Transactional
    public ResendMailsResponse resend(UUID ownerId, List<UUID> requestedIds) {
        Instant now = clock.instant();
        Set<UUID> ids = new LinkedHashSet<>(requestedIds);
        Map<UUID, EmailMessage> locked = mailService.lockForResend(ownerId, ids).stream()
                .collect(Collectors.toMap(EmailMessage::getId, Function.identity()));
        Map<UUID, InvitationMailRow> rows = queryRepository.findByEmailIds(ownerId, ids).stream()
                .collect(Collectors.toMap(InvitationMailRow::emailId, Function.identity()));

        List<FieldErrorItem> errors = new ArrayList<>();
        Set<UUID> reported = new HashSet<>();
        for (int i = 0; i < requestedIds.size(); i++) {
            UUID id = requestedIds.get(i);
            if (!reported.add(id)) {
                continue;
            }
            String code = notResendableReason(locked.get(id), rows.get(id), now);
            if (code != null) {
                errors.add(new FieldErrorItem("emailIds[" + i + "]", code, message(code)));
            }
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "MAILS_NOT_RESENDABLE",
                    "Seçilen maillerin bazıları yeniden gönderilemez", errors);
        }

        List<ResendMailsResponse.Item> items = new ArrayList<>();
        for (UUID id : ids) {
            InvitationMailRow row = rows.get(id);
            Invitation invitation = invitationRepository.findById(row.invitationId()).orElseThrow();
            String token = tokenGenerator.generate();
            invitation.rotateToken(tokenGenerator.hash(token));
            String url = frontend.invitationUrl(token);
            if (frontend.logInvitationLinks()) {
                log.info("Davet linki (yeniden): survey={} user={} url={}", row.surveyId(), row.recipientEmail(), url);
            }
            QueuedEmail queued = mailService.requeue(locked.get(id),
                    emailComposer.compose(row.recipientName(), row.surveyTitle(), row.surveyEndsAt(), url));
            items.add(new ResendMailsResponse.Item(queued.emailId(), queued.status()));
        }
        return new ResendMailsResponse(items.size(), items);
    }

    private MailListItemResponse toItem(InvitationMailRow row, Instant now) {
        boolean stuck = isStuck(row, now);
        return new MailListItemResponse(row.emailId(), row.invitationId(), row.surveyId(), row.surveyTitle(),
                row.recipientName(), row.recipientEmail(), row.emailStatus(), row.invitationStatus(),
                row.attemptCount(), row.lastError(), row.lastAttemptAt(), row.sentAt(),
                stuck, notResendableReason(row.emailStatus(), stuck, row, now) == null);
    }

    /** Listedeki {@code resendable} alanıyla aynı kural; uygun değilse hata kodu döner. */
    private String notResendableReason(EmailMessage message, InvitationMailRow row, Instant now) {
        if (message == null || row == null) {
            return "NOT_FOUND";
        }
        return notResendableReason(message.getStatus(), isStuck(row, now), row, now);
    }

    private String notResendableReason(EmailStatus status, boolean stuck, InvitationMailRow row, Instant now) {
        if (status != EmailStatus.FAILED && !stuck) {
            return "NOT_FAILED";
        }
        boolean invitationOpen = row.surveyStatus() == SurveyStatus.PUBLISHED
                && row.surveyEndsAt() != null && row.surveyEndsAt().isAfter(now)
                && row.invitationStatus() != InvitationStatus.COMPLETED
                && row.invitationStatus() != InvitationStatus.CANCELLED;
        return invitationOpen ? null : "INVITATION_CLOSED";
    }

    private boolean isStuck(InvitationMailRow row, Instant now) {
        return row.emailStatus() == EmailStatus.SENDING && row.emailUpdatedAt() != null
                && row.emailUpdatedAt().isBefore(now.minus(mailProperties.stuckAfter()));
    }

    private static String message(String code) {
        return switch (code) {
            case "NOT_FOUND" -> "Mail bulunamadı";
            case "NOT_FAILED" -> "Mail gönderilemeyen durumda değil";
            default -> "Davet artık geçerli değil (anket kapalı, süresi dolmuş veya yanıtlanmış)";
        };
    }
}
