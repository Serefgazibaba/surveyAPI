package com.definex.survey.config;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * CORS ayarları ({@code app.security.cors}). Origin listesi {@code CORS_ALLOWED_ORIGINS} ile virgülle ayrılmış verilir.
 */
@Validated
@ConfigurationProperties(prefix = "app.security.cors")
public record CorsProperties(@NotEmpty List<String> allowedOrigins) {
}
