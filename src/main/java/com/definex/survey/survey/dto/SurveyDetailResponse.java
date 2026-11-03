package com.definex.survey.survey.dto;

import com.definex.survey.survey.entity.ResponseMode;
import com.definex.survey.survey.entity.SurveyAction;
import com.definex.survey.survey.entity.SurveyStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SurveyDetailResponse(
        UUID id,
        String title,
        String description,
        SurveyStatus status,
        ResponseMode responseMode,
        Instant endsAt,
        Instant publishedAt,
        long participantCount,
        List<SurveyAction> allowedActions,
        Instant createdAt,
        Instant updatedAt,
        List<QuestionResponse> questions) {
}
