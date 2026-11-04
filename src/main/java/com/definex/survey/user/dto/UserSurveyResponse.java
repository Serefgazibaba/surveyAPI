package com.definex.survey.user.dto;

import com.definex.survey.survey.entity.QuestionType;
import com.definex.survey.survey.entity.ResponseMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Katılımcının dolduracağı anket formu (S-08).
 *
 * @param anonymousNotice anonim modda formun başında gösterilecek metin (FR-67); kayıtlı modda {@code null}
 * @param draft           ara kayıt; yoksa {@code null} (kaldığı yerden devam, FR-63)
 */
public record UserSurveyResponse(
        UUID id,
        String title,
        String description,
        ResponseMode responseMode,
        String anonymousNotice,
        Instant endsAt,
        int totalQuestionCount,
        List<Question> questions,
        UserDraft draft) {

    public record Question(UUID id, int number, String text, QuestionType type, boolean required,
                           Map<String, Object> settings, List<Option> options) {
    }

    public record Option(UUID id, String label) {
    }
}
