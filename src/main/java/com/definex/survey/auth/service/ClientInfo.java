package com.definex.survey.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

/**
 * İsteği yapan istemcinin IP ve user-agent bilgisi.
 * IP olarak {@code remoteAddr} kullanılır; {@code X-Forwarded-For} istemci tarafından taklit edilebildiği için okunmaz.
 * Uygulama bir reverse proxy arkasına alınırsa {@code server.forward-headers-strategy} ayarlanmalıdır.
 */
public record ClientInfo(String ipAddress, String userAgent) {

    private static final int MAX_USER_AGENT = 512;

    public static ClientInfo from(HttpServletRequest request) {
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        if (userAgent != null && userAgent.length() > MAX_USER_AGENT) {
            userAgent = userAgent.substring(0, MAX_USER_AGENT);
        }
        return new ClientInfo(request.getRemoteAddr(), userAgent);
    }
}
