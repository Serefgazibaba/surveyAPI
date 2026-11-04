package com.definex.survey.mail.service;

import com.definex.survey.mail.entity.EmailMessage;
import com.definex.survey.mail.entity.EmailStatus;
import com.definex.survey.mail.repository.EmailMessageRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mail modülünün dışa açık API'si. Mailler çağıranın transaction'ında kaydedilir ve transaction commit edilince
 * arka planda bir kez gönderilir ({@link EmailDispatchListener}). Otomatik yeniden deneme yoktur; başarısız
 * mailler admin tarafından {@link #requeue} ile yeniden gönderilir.
 */
@Service
public class MailService {

    private final EmailMessageRepository repository;
    private final ApplicationEventPublisher events;

    public MailService(EmailMessageRepository repository, ApplicationEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    /** Maili kuyruğa alır. Çağıranın transaction'ı geri alınırsa mail de kaydedilmez ve gönderilmez. */
    @Transactional(propagation = Propagation.MANDATORY)
    public QueuedEmail queue(EmailRequest request) {
        EmailContent content = request.content();
        EmailMessage message = repository.save(new EmailMessage(request.type(), request.ownerId(), request.surveyId(),
                request.invitationId(), request.recipientEmail(), request.recipientName(),
                content.subject(), content.html(), content.text()));
        events.publishEvent(new EmailQueuedEvent(message.getId()));
        return new QueuedEmail(message.getId(), message.getStatus());
    }

    /** Admin'e ait mailleri satır kilidiyle getirir (yeniden gönderim kontrolü için). */
    @Transactional(propagation = Propagation.MANDATORY)
    public List<EmailMessage> lockForResend(UUID ownerId, Collection<UUID> emailIds) {
        return repository.findForUpdateByIdInAndOwnerId(emailIds, ownerId);
    }

    /** Maili yeni içerikle yeniden kuyruğa alır; commit edilince gönderilir. */
    @Transactional(propagation = Propagation.MANDATORY)
    public QueuedEmail requeue(EmailMessage message, EmailContent content) {
        message.requeue(content.subject(), content.html(), content.text());
        events.publishEvent(new EmailQueuedEvent(message.getId()));
        return new QueuedEmail(message.getId(), EmailStatus.QUEUED);
    }

    public record QueuedEmail(UUID emailId, EmailStatus status) {
    }
}
