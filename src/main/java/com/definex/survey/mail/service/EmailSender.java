package com.definex.survey.mail.service;

import com.definex.survey.config.MailProperties;
import com.definex.survey.mail.entity.EmailMessage;
import com.definex.survey.mail.repository.EmailMessageRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tek bir maili gönderir. SMTP çağrısı transaction dışında yapılır (DB bağlantısı SMTP'yi beklemez):
 * <ol>
 *   <li>kayıt {@code QUEUED} → {@code SENDING} olarak alınır (başka bir iş parçacığı almışsa çıkılır)</li>
 *   <li>JavaMailSender ile gönderilir (HTML + düz metin)</li>
 *   <li>sonuca göre {@code SENT} veya {@code FAILED} yazılır; iki durumda da gövde silinir</li>
 * </ol>
 */
@Component
class EmailSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final EmailMessageRepository repository;
    private final JavaMailSender mailSender;
    private final TransactionTemplate transactions;
    private final MailProperties properties;
    private final String smtpUsername;
    private final Clock clock;

    EmailSender(EmailMessageRepository repository, JavaMailSender mailSender, TransactionTemplate transactions,
                MailProperties properties, @Value("${spring.mail.username:}") String smtpUsername, Clock clock) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.transactions = transactions;
        this.properties = properties;
        this.smtpUsername = smtpUsername;
        this.clock = clock;
    }

    void send(UUID emailId) {
        EmailMessage message = transactions.execute(status ->
                repository.claim(emailId, clock.instant()) == 1 ? repository.findById(emailId).orElse(null) : null);
        if (message == null) {
            return; // başka bir iş parçacığı aldı veya artık kuyrukta değil
        }

        String error = null;
        try {
            mailSender.send(build(message));
        } catch (MailAuthenticationException e) {
            error = "SMTP kimlik bilgileri hatalı (MAIL_USERNAME / MAIL_PASSWORD)";
            log.error("Mail gönderilemedi, {}: id={}", error, emailId);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.warn("Mail gönderilemedi: id={} hata={}", emailId, error);
        }

        String failure = error;
        transactions.executeWithoutResult(status -> repository.findById(emailId).ifPresent(saved -> {
            if (failure == null) {
                saved.markSent(clock.instant());
            } else {
                saved.markFailed(failure);
            }
        }));
        if (failure == null) {
            log.info("Mail gönderildi: id={} type={}", emailId, message.getType());
        }
    }

    private MimeMessage build(EmailMessage message) throws MessagingException, UnsupportedEncodingException {
        MimeMessage mime = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mime, true, "UTF-8");
        String from = properties.from() == null || properties.from().isBlank() ? smtpUsername : properties.from();
        helper.setFrom(from, properties.fromName());
        helper.setTo(message.getRecipientEmail());
        helper.setSubject(message.getSubject());
        helper.setText(message.getBodyText(), message.getBodyHtml());
        return mime;
    }
}
