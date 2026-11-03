package com.definex.survey.security;

import com.definex.survey.config.JwtProperties;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Access token üretimi. Admin ve katılımcı token'ları aynı formatta üretilir; ayrım {@code roles} claim'iyle yapılır.
 * Refresh token'ın saklanması ve rotation'ı auth modülünün sorumluluğundadır.
 */
@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public JwtTokenService(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    /**
     * @param subject     admin için kullanıcı id'si, katılımcı için davet id'si
     * @param roles       {@link Roles} sabitleri
     * @param extraClaims ek claim'ler (ör. katılımcı için {@code sid} = anket id'si); boş olabilir
     */
    public AccessToken issueAccessToken(String subject, Collection<String> roles, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.accessTokenTtl());

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(subject)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(JwtConfig.ROLES_CLAIM, List.copyOf(roles));
        extraClaims.forEach(claims::claim);

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new AccessToken(token, expiresAt, properties.accessTokenTtl().toSeconds());
    }

    public record AccessToken(String value, Instant expiresAt, long expiresInSeconds) {
    }
}
