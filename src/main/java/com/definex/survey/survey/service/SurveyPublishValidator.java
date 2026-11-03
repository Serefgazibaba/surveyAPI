package com.definex.survey.survey.service;

import com.definex.survey.exception.FieldErrorItem;
import com.definex.survey.survey.entity.Question;
import com.definex.survey.survey.entity.QuestionOption;
import com.definex.survey.survey.entity.QuestionType;
import com.definex.survey.survey.entity.Survey;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Yayınlama tamlık kuralları. Taslak eksik kaydedilebildiği için kurallar burada kontrol edilir; tüm eksikler
 * tek seferde döner.
 */
final class SurveyPublishValidator {

    private SurveyPublishValidator() {
    }

    static List<FieldErrorItem> validate(Survey survey, Instant now) {
        List<FieldErrorItem> errors = new ArrayList<>();
        if (survey.getEndsAt() == null) {
            errors.add(new FieldErrorItem("endsAt", "END_DATE_REQUIRED", "Bitiş tarihi girilmelidir"));
        } else if (!survey.getEndsAt().isAfter(now)) {
            errors.add(new FieldErrorItem("endsAt", "END_DATE_IN_PAST", "Bitiş tarihi gelecekte olmalıdır"));
        }
        if (survey.getResponseMode() == null) {
            errors.add(new FieldErrorItem("responseMode", "RESPONSE_MODE_REQUIRED", "Yanıt modu seçilmelidir"));
        }

        List<Question> questions = survey.getQuestions();
        if (questions.isEmpty()) {
            errors.add(new FieldErrorItem("questions", "SURVEY_HAS_NO_QUESTIONS", "Ankete en az bir soru eklenmelidir"));
        }
        for (int i = 0; i < questions.size(); i++) {
            validateQuestion(questions.get(i), "questions[" + i + "]", errors);
        }
        return errors;
    }

    private static void validateQuestion(Question question, String path, List<FieldErrorItem> errors) {
        if (isBlank(question.getText())) {
            errors.add(new FieldErrorItem(path + ".text", "TEXT_REQUIRED", "Soru metni girilmelidir"));
        }
        QuestionType type = question.getType();
        if (type == null) {
            errors.add(new FieldErrorItem(path + ".type", "TYPE_REQUIRED", "Soru tipi seçilmelidir"));
            return;
        }
        if (!type.hasOptions()) {
            return;
        }

        List<QuestionOption> options = question.getOptions();
        long labelled = options.stream().filter(option -> !isBlank(option.getLabel())).count();
        if (labelled < 2) {
            errors.add(new FieldErrorItem(path + ".options", "MIN_TWO_OPTIONS", "En az iki seçenek girilmelidir"));
        }
        for (int j = 0; j < options.size(); j++) {
            if (isBlank(options.get(j).getLabel())) {
                errors.add(new FieldErrorItem(path + ".options[" + j + "].label", "LABEL_REQUIRED",
                        "Seçenek metni girilmelidir"));
            }
        }
        if (type == QuestionType.MULTIPLE_CHOICE && question.getSettings() != null
                && question.getSettings().get("maxSelect") instanceof Number maxSelect
                && maxSelect.intValue() > options.size()) {
            errors.add(new FieldErrorItem(path + ".settings.maxSelect", "MAX_SELECT_EXCEEDS_OPTIONS",
                    "En fazla seçim sayısı seçenek sayısından büyük olamaz"));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
