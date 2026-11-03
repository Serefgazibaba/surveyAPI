package com.definex.survey.survey.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * @param userIds davet edilecek katılımcılar ({@code GET /admin/getAllUsers}); tekrarlananlar tekilleştirilir
 */
public record PublishSurveyRequest(
        @NotNull UUID surveyId,
        @NotEmpty @Size(max = 1000) List<@NotNull UUID> userIds) {
}
