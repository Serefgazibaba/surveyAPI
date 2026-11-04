package com.definex.survey.report.dto;

import com.definex.survey.survey.entity.ResponseMode;
import com.definex.survey.survey.entity.SurveyStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Anlık olarak hesaplanan admin raporu.
 */
public record SurveyReportResponse(
        UUID surveyId,
        String title,
        SurveyStatus status,
        ResponseMode responseMode,
        Instant endsAt,
        Instant generatedAt,
        ReportSummaryResponse summary,
        boolean anonymityThresholdMet,
        int anonymityThreshold,
        List<Object> questions) {
}

