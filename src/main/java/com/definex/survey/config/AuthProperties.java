package com.definex.survey.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Oturum ve giriş denemesi kuralları ({@code app.security.auth}).
 *
 * @param sessionIdleTimeout          son refresh'ten sonra bu süre geçerse oturum düşer (FR-02)
 * @param sessionMaxLifetime          oturumun mutlak ömrü; refresh token da en geç bu ana kadar geçerlidir
 * @param maxFailedAttemptsPerIp      aynı (e-posta, IP) için kilit eşiği (FR-01)
 * @param maxFailedAttemptsPerAccount bir e-posta için tüm IP'lerden toplam kilit eşiği
 * @param lockDuration                kilit süresi; aynı zamanda hatalı deneme sayma penceresi
 */
@Validated
@ConfigurationProperties(prefix = "app.security.auth")
public record AuthProperties(
        @NotNull Duration sessionIdleTimeout,
        @NotNull Duration sessionMaxLifetime,
        @Min(1) int maxFailedAttemptsPerIp,
        @Min(1) int maxFailedAttemptsPerAccount,
        @NotNull Duration lockDuration) {
}
