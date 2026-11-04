package com.definex.survey.template.dto;

import com.definex.survey.survey.entity.QuestionType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TemplateDetailResponse(
        UUID id,
        String name,
        String description,
        int questionCount,
        Instant createdAt,
        Instant updatedAt,
        List<Question> questions) {

    /** @param number şablonda görünen soru numarası (1'den başlar) */
    public record Question(
            int number,
            String text,
            QuestionType type,
            boolean required,
            Map<String, Object> settings,
            List<Option> options) {
    }

    /** @param sortOrder 0'dan başlar */
    public record Option(String label, int sortOrder) {
    }
}
