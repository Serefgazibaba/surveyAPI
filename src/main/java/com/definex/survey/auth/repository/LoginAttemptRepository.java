package com.definex.survey.auth.repository;

import com.definex.survey.auth.entity.LoginAttempt;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    Optional<LoginAttempt> findByEmailAndIpAddress(String email, String ipAddress);

    /**
     * Hatalı denemeyi tek sorguda kaydeder (eşzamanlı isteklerde sayaç kaybolmaz).
     * Son hata {@code windowStart}'tan eskiyse sayaç 1'den başlar. Sayaç {@code maxPerIp}'e ulaşırsa
     * {@code lockedUntil} yazılır.
     *
     * @return güncel sayaç
     */
    @Query(value = """
            INSERT INTO login_attempts (id, email, ip_address, failed_count, locked_until, last_failed_at, created_at, updated_at)
            VALUES (gen_random_uuid(), :email, :ip, 1,
                    CASE WHEN 1 >= :maxPerIp THEN CAST(:lockedUntil AS timestamptz) END, :now, :now, :now)
            ON CONFLICT (email, ip_address) DO UPDATE SET
                failed_count = CASE WHEN login_attempts.last_failed_at < :windowStart THEN 1
                                    ELSE login_attempts.failed_count + 1 END,
                locked_until = CASE WHEN (CASE WHEN login_attempts.last_failed_at < :windowStart THEN 1
                                               ELSE login_attempts.failed_count + 1 END) >= :maxPerIp
                                    THEN CAST(:lockedUntil AS timestamptz)
                                    ELSE login_attempts.locked_until END,
                last_failed_at = :now,
                updated_at = :now
            RETURNING failed_count
            """, nativeQuery = true)
    int recordFailure(@Param("email") String email,
                      @Param("ip") String ipAddress,
                      @Param("now") Instant now,
                      @Param("windowStart") Instant windowStart,
                      @Param("maxPerIp") int maxPerIp,
                      @Param("lockedUntil") Instant lockedUntil);

    /** Bir e-posta için pencere içindeki toplam hatalı deneme ve en son hata zamanı (tüm IP'ler). */
    @Query("""
            select new com.definex.survey.auth.repository.AccountFailureSummary(
                coalesce(sum(a.failedCount), 0L), max(a.lastFailedAt))
            from LoginAttempt a
            where a.email = :email and a.lastFailedAt >= :windowStart
            """)
    AccountFailureSummary summarizeRecentFailures(@Param("email") String email,
                                                  @Param("windowStart") Instant windowStart);

    @Modifying
    @Query("delete from LoginAttempt a where a.email = :email and a.ipAddress = :ip")
    void deleteByEmailAndIpAddress(@Param("email") String email, @Param("ip") String ipAddress);
}
