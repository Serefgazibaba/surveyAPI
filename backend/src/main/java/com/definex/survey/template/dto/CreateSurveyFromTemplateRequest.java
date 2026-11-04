package com.definex.survey.template.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** @param title boşsa şablonun adı kullanılır */
public record CreateSurveyFromTemplateRequest(
        @NotNull UUID templateId,
        @Size(max = 200) String title) {
}
