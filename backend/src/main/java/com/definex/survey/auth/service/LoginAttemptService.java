package com.definex.survey.auth.service;

import com.definex.survey.auth.repository.AccountFailureSummary;
import com.definex.survey.auth.repository.LoginAttemptRepository;
import com.definex.survey.config.AuthProperties;
import com.definex.survey.exception.AccountLockedException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hatalı giriş sayacı ve kilit kuralları (FR-01).
 * <ul>
 *   <li>(e-posta, IP) için {@code maxFailedAttemptsPerIp} hatada yalnızca o IP kilitlenir.</li>
 *   <li>Bir e-posta için pencerede tüm IP'lerden toplam {@code maxFailedAttemptsPerAccount} hatada e-posta kilitlenir.</li>
 * </ul>
 */
@Service
public class LoginAttemptService {

    private final LoginAttemptRepository repository;
    private final AuthProperties properties;

    public LoginAttemptService(LoginAttemptRepository repository, AuthProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /**
     * Kilit varsa {@link AccountLockedException} fırlatır. Transaction açmaz; çağıranın transaction'ını
     * rollback-only işaretlememesi için bilinçli olarak {@code @Transactional} değildir.
     */
    public void assertNotLocked(String email, String ipAddress, Instant now) {
        repository.findByEmailAndIpAddress(email, ipAddress)
                .map(attempt -> attempt.getLockedUntil())
                .filter(lockedUntil -> lockedUntil != null && lockedUntil.isAfter(now))
                .ifPresent(lockedUntil -> {
                    throw new AccountLockedException(Duration.between(now, lockedUntil));
                });

        AccountFailureSummary summary = repository.summarizeRecentFailures(email, windowStart(now));
        if (summary.totalFailures() >= properties.maxFailedAttemptsPerAccount()) {
            Instant unlockAt = summary.lastFailedAt().plus(properties.lockDuration());
            throw new AccountLockedException(Duration.between(now, unlockAt));
        }
    }

    /**
     * Hatalı denemeyi kaydeder. Çağıran exception fırlatacağı için ayrı transaction'da commit edilir.
     *
     * @return bu deneme bir kilit eşiğini doldurduysa {@code true}
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordFailure(String email, String ipAddress, Instant now) {
        int ipFailures = repository.recordFailure(email, ipAddress, now, windowStart(now),
                properties.maxFailedAttemptsPerIp(), now.plus(properties.lockDuration()));
        if (ipFailures >= properties.maxFailedAttemptsPerIp()) {
            return true;
        }
        return repository.summarizeRecentFailures(email, windowStart(now)).totalFailures()
                >= properties.maxFailedAttemptsPerAccount();
    }

    /** Başarılı girişte yalnızca bu (e-posta, IP) kaydı silinir; diğer IP'lerin kayıtları kalır. */
    @Transactional
    public void recordSuccess(String email, String ipAddress) {
        repository.deleteByEmailAndIpAddress(email, ipAddress);
    }

    private Instant windowStart(Instant now) {
        return now.minus(properties.lockDuration());
    }
}
