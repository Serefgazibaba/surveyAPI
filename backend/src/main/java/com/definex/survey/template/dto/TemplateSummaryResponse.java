package com.definex.survey.template.dto;

import java.time.Instant;
import java.util.UUID;

public record TemplateSummaryResponse(
        UUID id,
        String name,
        String description,
        int questionCount,
        Instant createdAt,
        Instant updatedAt) {
}
