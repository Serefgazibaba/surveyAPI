package com.definex.survey.template.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Anketi şablon olarak kaydet (FR-30).
 *
 * @param name        boşsa anketin başlığı kullanılır
 * @param description boşsa anketin açıklaması kullanılır
 */
public record SaveAsTemplateRequest(
        @NotNull UUID surveyId,
        @Size(max = 200) String name,
        @Size(max = 2000) String description) {
}
