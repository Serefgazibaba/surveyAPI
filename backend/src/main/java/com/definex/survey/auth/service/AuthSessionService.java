package com.definex.survey.auth.service;

import com.definex.survey.auth.entity.AuthSession;
import com.definex.survey.auth.entity.RevokeReason;
import com.definex.survey.auth.entity.User;
import com.definex.survey.auth.repository.AuthSessionRepository;
import com.definex.survey.config.AuthProperties;
import com.definex.survey.security.SecureTokenGenerator;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Oturum ve refresh token işleri (oluşturma, rotation, iptal). Rol kuralları çağıran service'tedir.
 * Metotlar exception fırlatmaz, sonuç döner; böylece çağıranın transaction'ı rollback-only işaretlenmez
 * ve iptal kayıtları (ör. {@code TOKEN_REUSE}) commit edilir.
 */
@Service
public class AuthSessionService {

    private final AuthSessionRepository repository;
    private final SecureTokenGenerator tokenGenerator;
    private final AuthProperties properties;

    public AuthSessionService(AuthSessionRepository repository, SecureTokenGenerator tokenGenerator,
                              AuthProperties properties) {
        this.repository = repository;
        this.tokenGenerator = tokenGenerator;
        this.properties = properties;
    }

    @Transactional
    public IssuedSession createSession(User user, ClientInfo client, Instant now) {
        String refreshToken = tokenGenerator.generate();
        AuthSession session = repository.save(new AuthSession(user, client.ipAddress(), client.userAgent(),
                tokenGenerator.hash(refreshToken), now, properties.sessionMaxLifetime()));
        return new IssuedSession(session, refreshToken);
    }

    /**
     * Refresh token'ı doğrular ve döndürür (rotation).
     *
     * @return yeni refresh token'lı oturum; token geçersizse veya oturum düştüyse boş
     */
    @Transactional
    public Optional<IssuedSession> rotate(String rawRefreshToken, Instant now) {
        String hash = tokenGenerator.hash(rawRefreshToken);

        Optional<AuthSession> current = repository.findForUpdateByRefreshTokenHash(hash);
        if (current.isEmpty()) {
            // Bir önceki token tekrar geldiyse token çalınmış olabilir: oturumu iptal et.
            repository.findForUpdateByPreviousRefreshTokenHash(hash)
                    .ifPresent(session -> session.revoke(RevokeReason.TOKEN_REUSE, now));
            return Optional.empty();
        }

        AuthSession session = current.get();
        if (session.isRevoked()) {
            return Optional.empty();
        }
        if (session.isExpired(now)) {
            session.revoke(RevokeReason.EXPIRED, now);
            return Optional.empty();
        }
        if (session.isIdle(now, properties.sessionIdleTimeout())) {
            session.revoke(RevokeReason.IDLE_TIMEOUT, now);
            return Optional.empty();
        }

        String newRefreshToken = tokenGenerator.generate();
        session.rotateRefreshToken(tokenGenerator.hash(newRefreshToken), now);
        return Optional.of(new IssuedSession(session, newRefreshToken));
    }

    /** İdempotent; oturum yoksa bir şey yapmaz. */
    @Transactional
    public void revoke(UUID sessionId, RevokeReason reason, Instant now) {
        repository.findById(sessionId).ifPresent(session -> session.revoke(reason, now));
    }

    /**
     * Refresh token'ın kalan geçerlilik süresi (saniye): hareketsizlik penceresi ile mutlak ömürden kısa olanı.
     */
    public long refreshExpiresInSeconds(AuthSession session, Instant now) {
        Instant idleDeadline = session.getLastActivityAt().plus(properties.sessionIdleTimeout());
        Instant deadline = idleDeadline.isBefore(session.getExpiresAt()) ? idleDeadline : session.getExpiresAt();
        return Math.max(0, deadline.getEpochSecond() - now.getEpochSecond());
    }

    /** Oturum ve istemciye bir kez verilen ham refresh token. */
    public record IssuedSession(AuthSession session, String refreshToken) {
    }
}
