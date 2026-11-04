package com.definex.survey.report.controller;

import com.definex.survey.report.dto.SurveyReportResponse;
import com.definex.survey.report.service.ReportSurveyService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/surveys/{surveyId}/report")
@Validated
public class AdminReportController {

    private final ReportSurveyService reportSurveyService;

    public AdminReportController(ReportSurveyService reportSurveyService) {
        this.reportSurveyService = reportSurveyService;
    }

    @GetMapping
    public SurveyReportResponse get(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID surveyId,
            @RequestParam(defaultValue = "20") @Min(0) @Max(100) int textAnswerLimit) {
        return reportSurveyService.get(UUID.fromString(jwt.getSubject()), surveyId, textAnswerLimit);
    }
}
