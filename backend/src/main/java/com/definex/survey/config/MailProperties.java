package com.definex.survey.config;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Mail ayarları ({@code app.mail}). SMTP bağlantısı {@code spring.mail.*} ile yapılandırılır.
 *
 * @param from       gönderen adresi; boşsa {@code spring.mail.username} kullanılır
 * @param fromName   gönderen görünen adı
 * @param stuckAfter bu süreden uzun {@code SENDING} durumunda kalan mail takılı sayılır
 */
@Validated
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(String from, String fromName, @NotNull Duration stuckAfter) {
}
