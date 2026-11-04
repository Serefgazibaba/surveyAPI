package com.definex.survey.survey.dto;

import com.definex.survey.survey.entity.QuestionType;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** @param number anket genelinde görünen soru numarası (1'den başlar) */
public record QuestionResponse(
        UUID id,
        int number,
        String text,
        QuestionType type,
        boolean required,
        Map<String, Object> settings,
        List<OptionResponse> options) {
}
