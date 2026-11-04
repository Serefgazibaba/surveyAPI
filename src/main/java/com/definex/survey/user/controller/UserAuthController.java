package com.definex.survey.user.controller;

import com.definex.survey.user.dto.UserLoginRequest;
import com.definex.survey.user.dto.UserLoginResponse;
import com.definex.survey.user.service.ParticipantAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/auth")
@Tag(name = "User - Auth", description = "Katılımcının davet linkiyle oturum açması")
public class UserAuthController {

    private final ParticipantAuthService participantAuthService;

    public UserAuthController(ParticipantAuthService participantAuthService) {
        this.participantAuthService = participantAuthService;
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(operationId = "userLogin", summary = "Katılımcı girişi (davet token'ı ile)",
            description = """
                    Davet linkindeki token'ı ankete bağlı bir katılımcı token'ına çevirir. Şifre ve refresh yoktur;
                    token anketin bitiş tarihine kadar geçerlidir. Süresi dolmuş anket 410, yanıtlanmış davet 409,
                    geçersiz/iptal davet 401 döner.""")
    public ResponseEntity<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(participantAuthService.login(request));
    }
}
