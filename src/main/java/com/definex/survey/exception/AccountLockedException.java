package com.definex.survey.exception;

import java.time.Duration;
import org.springframework.http.HttpStatus;

/** Çok sayıda hatalı giriş denemesi: 423 + {@code Retry-After} (FR-01). */
public class AccountLockedException extends ApiException {

    private final Duration retryAfter;

    public AccountLockedException(Duration retryAfter) {
        super(HttpStatus.LOCKED, "ACCOUNT_LOCKED", "Hesap geçici olarak kilitlendi");
        this.retryAfter = retryAfter;
    }

    /** En az 1 saniye. */
    public long getRetryAfterSeconds() {
        return Math.max(1, (retryAfter.toMillis() + 999) / 1000);
    }
}
