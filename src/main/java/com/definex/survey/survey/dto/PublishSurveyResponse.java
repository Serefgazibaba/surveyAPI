package com.definex.survey.survey.dto;

import com.definex.survey.survey.entity.SurveyStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PublishSurveyResponse(
        UUID surveyId,
        SurveyStatus status,
        Instant publishedAt,
        int invitationCount,
        List<InvitationLinkResponse> invitations) {
}
