package com.definex.survey.mail.repository;

import com.definex.survey.mail.entity.EmailMessage;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface EmailMessageRepository extends JpaRepository<EmailMessage, UUID> {

    /**
     * {@code QUEUED} kaydı {@code SENDING} yapar ve deneme sayısını artırır. Koşullu güncelleme olduğu için aynı mail
     * iki iş parçacığı tarafından birden gönderilmez.
     *
     * @return 1 ise kayıt bu çağrı tarafından alındı
     */
    @Modifying
    @Query("""
            update EmailMessage m
            set m.status = com.definex.survey.mail.entity.EmailStatus.SENDING,
                m.attemptCount = m.attemptCount + 1, m.lastAttemptAt = :now, m.updatedAt = :now
            where m.id = :id and m.status = com.definex.survey.mail.entity.EmailStatus.QUEUED
            """)
    int claim(@Param("id") UUID id, @Param("now") Instant now);

    /** Yeniden gönderim için satır kilidiyle getirir; aynı anda iki yeniden gönderim isteği çakışmaz. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select m from EmailMessage m where m.id in :ids and m.ownerId = :ownerId")
    List<EmailMessage> findForUpdateByIdInAndOwnerId(@Param("ids") Collection<UUID> ids,
                                                     @Param("ownerId") UUID ownerId);
}
