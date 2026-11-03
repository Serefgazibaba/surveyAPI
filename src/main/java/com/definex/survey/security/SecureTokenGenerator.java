package com.definex.survey.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Opak (anlamsız) token üretimi ve hash'lenmesi. Refresh token gibi değerlerin yalnızca hash'i saklanır;
 * ham değer istemciye bir kez verilir.
 */
@Component
public class SecureTokenGenerator {

    private static final int TOKEN_BYTES = 32; // 256 bit

    private final SecureRandom random = new SecureRandom();

    /** URL-safe, padding'siz base64 (43 karakter). */
    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 hex (64 karakter). Token yüksek entropili olduğu için salt gerekmez. */
    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 desteklenmiyor", e);
        }
    }
}
