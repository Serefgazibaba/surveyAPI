package com.definex.survey.auth.service;

import com.definex.survey.auth.dto.AdminAuthTokenResponse;
import com.definex.survey.auth.dto.AdminLoginRequest;
import com.definex.survey.auth.dto.AdminUserResponse;
import com.definex.survey.auth.entity.AuthSession;
import com.definex.survey.auth.entity.RevokeReason;
import com.definex.survey.auth.entity.User;
import com.definex.survey.auth.entity.UserRole;
import com.definex.survey.auth.repository.UserRepository;
import com.definex.survey.auth.service.AuthSessionService.IssuedSession;
import com.definex.survey.exception.AccountLockedException;
import com.definex.survey.exception.ApiException;
import com.definex.survey.config.AuthProperties;
import com.definex.survey.security.JwtTokenService;
import com.definex.survey.security.JwtTokenService.AccessToken;
import com.definex.survey.security.Roles;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin girişi, oturum yenileme ve çıkış. Yalnızca {@code ADMIN} rolündeki aktif kullanıcılar giriş yapabilir.
 *
 * <p>Metotlar {@code noRollbackFor = ApiException.class} ile çalışır: hata yanıtı dönerken yapılan yazmalar
 * (ör. oturum iptali) geri alınmaz.
 */
@Service
public class AdminAuthService {

    /** JWT'de oturum id'sini taşıyan claim. */
    public static final String SESSION_CLAIM = "sid";

    private static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    private static final String SESSION_EXPIRED = "SESSION_EXPIRED";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final AuthSessionService sessionService;
    private final JwtTokenService jwtTokenService;
    private final AuthProperties authProperties;
    private final Clock clock;
    /** Kullanıcı bulunamadığında da BCrypt çalışsın diye (yanıt süresinden hesap varlığı anlaşılmasın). */
    private final String dummyPasswordHash;

    public AdminAuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            LoginAttemptService loginAttemptService, AuthSessionService sessionService,
                            JwtTokenService jwtTokenService, AuthProperties authProperties, Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.sessionService = sessionService;
        this.jwtTokenService = jwtTokenService;
        this.authProperties = authProperties;
        this.clock = clock;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AdminAuthTokenResponse login(AdminLoginRequest request, ClientInfo client) {
        Instant now = clock.instant();
        String email = normalizeEmail(request.email());

        loginAttemptService.assertNotLocked(email, client.ipAddress(), now);

        Optional<User> user = userRepository.findByEmail(email);
        String hash = user.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);

        if (user.isEmpty() || !passwordMatches) {
            if (loginAttemptService.recordFailure(email, client.ipAddress(), now)) {
                throw new AccountLockedException(authProperties.lockDuration());
            }
            throw invalidCredentials();
        }
        // Şifre doğru ama admin değil veya pasif: genel hata (rol/hesap varlığı sızmaz).
        if (!isActiveAdmin(user.get())) {
            throw invalidCredentials();
        }

        loginAttemptService.recordSuccess(email, client.ipAddress());
        IssuedSession issued = sessionService.createSession(user.get(), client, now);
        return toResponse(issued, now);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AdminAuthTokenResponse refresh(String refreshToken) {
        Instant now = clock.instant();
        IssuedSession issued = sessionService.rotate(refreshToken, now).orElseThrow(this::sessionExpired);

        AuthSession session = issued.session();
        if (!isActiveAdmin(session.getUser())) {
            session.revoke(RevokeReason.ACCOUNT_DISABLED, now);
            throw sessionExpired();
        }
        return toResponse(issued, now);
    }

    /** İdempotent: oturum zaten kapalıysa da başarılı döner. */
    @Transactional
    public void logout(UUID sessionId, UUID userId, String refreshToken) {
        Instant now = clock.instant();
        if (sessionId != null) {
            sessionService.revoke(sessionId, RevokeReason.LOGOUT, now);
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            sessionService.revokeByRefreshToken(refreshToken, userId, RevokeReason.LOGOUT, now);
        }
    }

    private AdminAuthTokenResponse toResponse(IssuedSession issued, Instant now) {
        AuthSession session = issued.session();
        User user = session.getUser();
        AccessToken accessToken = jwtTokenService.issueAccessToken(user.getId().toString(), List.of(Roles.ADMIN),
                Map.of(SESSION_CLAIM, session.getId().toString()));
        return new AdminAuthTokenResponse(
                accessToken.value(),
                AdminAuthTokenResponse.BEARER,
                accessToken.expiresInSeconds(),
                issued.refreshToken(),
                sessionService.refreshExpiresInSeconds(session, now),
                AdminUserResponse.from(user));
    }

    private static boolean isActiveAdmin(User user) {
        return user.getRole() == UserRole.ADMIN && user.isActive();
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS, "E-posta veya şifre hatalı");
    }

    private ApiException sessionExpired() {
        return new ApiException(HttpStatus.UNAUTHORIZED, SESSION_EXPIRED, "Oturum sona erdi");
    }
}
