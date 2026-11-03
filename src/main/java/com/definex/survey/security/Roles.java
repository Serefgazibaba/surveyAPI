package com.definex.survey.security;

/**
 * Rol adları. JWT'de {@code roles} claim'i içinde prefix'siz taşınır ({@code ["ADMIN"]});
 * Spring Security'de {@code ROLE_} prefix'i ile authority'ye dönüşür.
 *
 * <p>Kullanım: {@code @PreAuthorize("hasRole('ADMIN')")} veya {@code hasRole(Roles.ADMIN)}.
 */
public final class Roles {

    /** Anket sahibi; {@code /admin/**} uçları. */
    public static final String ADMIN = "ADMIN";

    /** Davet tokenı ile giriş yapan katılımcı; {@code /user/**} uçları. */
    public static final String USER = "USER";

    private Roles() {
    }
}
