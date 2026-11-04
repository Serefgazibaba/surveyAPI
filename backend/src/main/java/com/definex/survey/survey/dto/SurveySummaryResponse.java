package com.definex.survey.survey.dto;

import com.definex.survey.survey.entity.ResponseMode;
import com.definex.survey.survey.entity.SurveyAction;
import com.definex.survey.survey.entity.SurveyStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SurveySummaryResponse(
        UUID id,
        String title,
        SurveyStatus status,
        ResponseMode responseMode,
        Instant endsAt,
        long questionCount,
        long participantCount,
        List<SurveyAction> allowedActions,
        Instant createdAt,
        Instant updatedAt) {
}
