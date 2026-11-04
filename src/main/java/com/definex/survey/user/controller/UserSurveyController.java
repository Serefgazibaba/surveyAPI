package com.definex.survey.user.controller;

import com.definex.survey.user.dto.UserSurveyResponse;
import com.definex.survey.user.service.ParticipantPrincipal;
import com.definex.survey.user.service.UserSurveyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
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

    public UserSurveyController(UserSurveyService userSurveyService) {
        this.userSurveyService = userSurveyService;
    }

    @GetMapping("/{surveyId}")
    @Operation(operationId = "userGetSurvey", summary = "Anket formunu getir",
            description = "Katılımcı token'ının bağlı olduğu anketin formunu döner; başka anket için 404.")
    public UserSurveyResponse getSurvey(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID surveyId) {
        return userSurveyService.getSurvey(ParticipantPrincipal.from(jwt), surveyId);
    }
}
