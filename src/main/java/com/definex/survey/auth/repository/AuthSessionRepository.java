package com.definex.survey.auth.repository;

import com.definex.survey.auth.entity.AuthSession;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    /**
     * Refresh için oturumu satır kilidiyle getirir; aynı token'la gelen eşzamanlı refresh'ler sıraya girer.
     * Yalnızca bu oturum satırı kilitlenir (user join edilmez), diğer kullanıcılar ve oturumlar etkilenmez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select s from AuthSession s where s.refreshTokenHash = :hash")
    Optional<AuthSession> findForUpdateByRefreshTokenHash(@Param("hash") String refreshTokenHash);

    /** Yeniden kullanım tespiti: bir önceki refresh token tekrar gelmiş mi? */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000"))
    @Query("select s from AuthSession s where s.previousRefreshTokenHash = :hash")
    Optional<AuthSession> findForUpdateByPreviousRefreshTokenHash(@Param("hash") String refreshTokenHash);

    Optional<AuthSession> findByRefreshTokenHash(String refreshTokenHash);
}
