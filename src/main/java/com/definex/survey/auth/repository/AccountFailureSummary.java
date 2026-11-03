package com.definex.survey.auth.repository;

import java.time.Instant;

/**
 * Bir e-posta için pencere içindeki hatalı denemelerin özeti (tüm IP'ler).
 *
 * @param lastFailedAt pencerede hiç deneme yoksa {@code null}
 */
public record AccountFailureSummary(long totalFailures, Instant lastFailedAt) {
}
