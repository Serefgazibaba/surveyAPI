package com.definex.survey.survey.controller;

import com.definex.survey.mail.entity.EmailStatus;
import com.definex.survey.survey.dto.MailListResponse;
import com.definex.survey.survey.dto.ResendMailsRequest;
import com.definex.survey.survey.dto.ResendMailsResponse;
import com.definex.survey.survey.entity.InvitationStatus;
import com.definex.survey.survey.repository.InvitationMailQueryRepository.MailFilter;
import com.definex.survey.survey.service.InvitationMailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@Validated
@Tag(name = "Admin - Mails", description = "Davet mailleri: durum listesi ve yeniden gönderim")
public class AdminMailController {

    private final InvitationMailService invitationMailService;

    public AdminMailController(InvitationMailService invitationMailService) {
        this.invitationMailService = invitationMailService;
    }

    @GetMapping("/getMails")
    @Operation(operationId = "adminGetMails", summary = "Davet maillerini listele (filtreli)",
            description = """
                    Admin'in anketlerine ait davet maillerini mail ve davet durumuyla döner. Filtrelerin hepsi
                    opsiyoneldir ve birlikte kullanılabilir; ör. gönderilemeyenler için emailStatus=FAILED.
                    summary alanı durum filtresi uygulanmadan durum başına sayıları verir.""")
    public MailListResponse getMails(
            @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "QUEUED, SENDING, SENT, FAILED (çoklu)")
            @RequestParam(required = false) List<EmailStatus> emailStatus,
            @Parameter(description = "PENDING, STARTED, COMPLETED, CANCELLED (çoklu)")
            @RequestParam(required = false) List<InvitationStatus> invitationStatus,
            @RequestParam(required = false) UUID surveyId,
            @Parameter(description = "Alıcı adı veya e-postada arama")
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        MailFilter filter = new MailFilter(UUID.fromString(jwt.getSubject()), surveyId, q, emailStatus, invitationStatus);
        return invitationMailService.list(filter, page, size);
    }

    @PostMapping("/resendMails")
    @Operation(operationId = "adminResendMails", summary = "Seçilen mailleri yeniden gönder",
            description = """
                    Gönderilemeyen (FAILED) veya takılı (uzun süredir SENDING) mailleri yeniden gönderir. Her mail için
                    davet linki yenilenir; eski link geçersiz olur. Seçilenlerden biri bile uygun değilse hiçbiri
                    gönderilmez (422 MAILS_NOT_RESENDABLE).""")
    public ResendMailsResponse resendMails(@AuthenticationPrincipal Jwt jwt,
                                           @Valid @RequestBody ResendMailsRequest request) {
        return invitationMailService.resend(UUID.fromString(jwt.getSubject()), request.emailIds());
    }
}
