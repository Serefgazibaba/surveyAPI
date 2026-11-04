package com.definex.survey.mail.service;

import com.definex.survey.config.AsyncConfig;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Kuyruğa alınan maili, kaydı oluşturan transaction commit edildikten sonra arka planda gönderir.
 * Transaction geri alınırsa event işlenmez; böylece hiç kaydedilmemiş bir mail gönderilmez.
 */
@Component
public class EmailDispatchListener {

    private final EmailSender sender;

    EmailDispatchListener(EmailSender sender) {
        this.sender = sender;
    }

    @Async(AsyncConfig.MAIL_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onQueued(EmailQueuedEvent event) {
        sender.send(event.emailId());
    }
}
