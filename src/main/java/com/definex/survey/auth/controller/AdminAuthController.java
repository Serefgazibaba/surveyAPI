package com.definex.survey.auth.controller;

import com.definex.survey.auth.dto.AdminAuthTokenResponse;
import com.definex.survey.auth.dto.AdminLoginRequest;
import com.definex.survey.auth.dto.LogoutRequest;
import com.definex.survey.auth.dto.RefreshTokenRequest;
import com.definex.survey.auth.service.AdminAuthService;
import com.definex.survey.auth.service.ClientInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/auth")
@Tag(name = "Admin - Auth", description = "Admin kimlik doğrulama")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Admin girişi", description = "E-posta ve şifre ile giriş; access ve refresh token döner.")
    public ResponseEntity<AdminAuthTokenResponse> login(@Valid @RequestBody AdminLoginRequest request,
                                                        HttpServletRequest httpRequest) {
        return noStore(adminAuthService.login(request, ClientInfo.from(httpRequest)));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Admin oturumunu yenile",
            description = "Refresh token tek kullanımlıktır; her çağrıda yeni token çifti döner.")
    public ResponseEntity<AdminAuthTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return noStore(adminAuthService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "Admin çıkışı", description = "Mevcut oturumu iptal eder. İdempotenttir.")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt,
                                       @Valid @RequestBody(required = false) LogoutRequest request) {
        String sessionId = jwt.getClaimAsString(AdminAuthService.SESSION_CLAIM);
        adminAuthService.logout(
                sessionId == null ? null : UUID.fromString(sessionId),
                UUID.fromString(jwt.getSubject()),
                request == null ? null : request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    /** Token içeren yanıtlar önbelleğe alınmaz. */
    private static <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
