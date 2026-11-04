package com.definex.survey.user.controller;

import com.definex.survey.user.dto.UserDraftRequest;
import com.definex.survey.user.dto.UserDraftResponse;
import com.definex.survey.user.dto.UserSubmitRequest;
import com.definex.survey.user.dto.UserSubmitResponse;
import com.definex.survey.user.dto.UserSurveyResponse;
import com.definex.survey.user.service.ParticipantPrincipal;
import com.definex.survey.user.service.UserResponseService;
import com.definex.survey.user.service.UserSurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user/surveys")
@Tag(name = "User - Surveys", description = "Katılımcı anket formu")
public class UserSurveyController {

    private final UserSurveyService userSurveyService;
    private final UserResponseService userResponseService;

    public UserSurveyController(UserSurveyService userSurveyService, UserResponseService userResponseService) {
        this.userSurveyService = userSurveyService;
        this.userResponseService = userResponseService;
    }

    @GetMapping("/{surveyId}")
    @Operation(operationId = "userGetSurvey", summary = "Anket formunu getir",
            description = "Katılımcı token'ının bağlı olduğu anketin formunu döner; başka anket için 404.")
    public UserSurveyResponse getSurvey(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID surveyId) {
        return userSurveyService.getSurvey(ParticipantPrincipal.from(jwt), surveyId);
    }

    @PutMapping("/{surveyId}/draft")
    @Operation(operationId = "userSaveDraft", summary = "Ara kayıt",
            description = """
                    Cevapları ara kayıt olarak saklar; mevcut ara kaydın tamamının yerine geçer. Zorunlu soru kontrolü
                    yapılmaz, yalnızca değerlerin soru tipine uygunluğu kontrol edilir (422 INVALID_ANSWERS).""")
    public UserDraftResponse saveDraft(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID surveyId,
                                       @Valid @RequestBody UserDraftRequest request) {
        return userResponseService.saveDraft(ParticipantPrincipal.from(jwt), surveyId, request);
    }

    @PostMapping("/{surveyId}/submit")
    @Operation(operationId = "userSubmitSurvey", summary = "Anketi gönder",
            description = """
                    Nihai cevapları kesinleştirir. Hatalı değer 422 INVALID_ANSWERS, eksik zorunlu soru 422
                    REQUIRED_ANSWERS_MISSING döner. Başarılı gönderimden sonra davet tamamlanır; aynı link ve token
                    ile sonraki istekler 409 ALREADY_RESPONDED alır.""")
    public UserSubmitResponse submit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID surveyId,
                                     @Valid @RequestBody UserSubmitRequest request) {
        return userResponseService.submit(ParticipantPrincipal.from(jwt), surveyId, request);
    }
}
