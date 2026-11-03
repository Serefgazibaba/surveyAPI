package com.definex.survey.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Ön yüz ayarları ({@code app.frontend}).
 *
 * @param baseUrl        davet linklerinin tabanı; link = {@code baseUrl + "/s/" + token}
 * @param logInvitationLinks davet linklerini log'a yazar; link token içerdiği için yalnızca dev'de açılır
 */
@Validated
@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProperties(@NotBlank String baseUrl, boolean logInvitationLinks) {

    public String invitationUrl(String token) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return base + "/s/" + token;
    }
}
