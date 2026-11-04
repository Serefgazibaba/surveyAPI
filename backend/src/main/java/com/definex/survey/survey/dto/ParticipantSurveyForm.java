package com.definex.survey.survey.dto;

import com.definex.survey.survey.entity.QuestionType;
import com.definex.survey.survey.entity.ResponseMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Katılımcıya gösterilecek anket formu; admin'e özel alanlar (durum, sahip, aksiyonlar) içermez. */
public record ParticipantSurveyForm(
        UUID id,
        String title,
        String description,
        ResponseMode responseMode,
        Instant endsAt,
        List<Question> questions) {

    /** @param number anket genelinde görünen soru numarası (1'den başlar) */
    public record Question(UUID id, int number, String text, QuestionType type, boolean required,
                           Map<String, Object> settings, List<Option> options) {
    }

    public record Option(UUID id, String label) {
    }
}
