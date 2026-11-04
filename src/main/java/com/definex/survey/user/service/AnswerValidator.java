package com.definex.survey.user.service;

import com.definex.survey.exception.FieldErrorItem;
import com.definex.survey.survey.dto.ParticipantSurveyForm;
import com.definex.survey.survey.dto.ParticipantSurveyForm.Question;
import com.definex.survey.user.dto.AnswerInput;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Cevapların soru tipine uygunluğu (ara kayıt ve gönderim ortak) ve zorunlu soru kontrolü (yalnızca gönderim).
 *
 * <pre>
 * LIKERT          numericValue 1..scale
 * SINGLE_CHOICE   en fazla 1 seçenek veya (allowOther ise) otherText; ikisi birlikte olamaz
 * MULTIPLE_CHOICE seçenek + (allowOther ise) otherText; toplam seçim minSelect..maxSelect
 * FREE_TEXT       text ≤ maxLength
 * </pre>
 * Seçenekler o soruya ait olmalı; tipe ait olmayan alan dolu gelemez. Hiçbir alanı dolu olmayan cevap yanıtlanmamış sayılır.
 */
final class AnswerValidator {

    private AnswerValidator() {
    }

    static List<FieldErrorItem> validate(ParticipantSurveyForm form, List<AnswerInput> answers) {
        Map<UUID, Question> questions = form.questions().stream()
                .collect(Collectors.toMap(Question::id, Function.identity()));
        List<FieldErrorItem> errors = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();

        for (int i = 0; i < answers.size(); i++) {
            AnswerInput answer = answers.get(i);
            String path = "answers[" + i + "]";
            Question question = questions.get(answer.questionId());
            if (question == null) {
                errors.add(error(path + ".questionId", "UNKNOWN_QUESTION", "Bu soru ankette yok"));
                continue;
            }
            if (!seen.add(answer.questionId())) {
                errors.add(error(path + ".questionId", "DUPLICATE_ANSWER", "Aynı soru birden fazla kez cevaplandı"));
                continue;
            }
            if (answer.isEmpty()) {
                continue;
            }
            switch (question.type()) {
                case LIKERT -> likert(question, answer, path, errors);
                case SINGLE_CHOICE -> choice(question, answer, path, errors, true);
                case MULTIPLE_CHOICE -> choice(question, answer, path, errors, false);
                case FREE_TEXT -> freeText(question, answer, path, errors);
            }
        }
        return errors;
    }

    /** Cevaplanmamış zorunlu sorular; {@code field} soru kimliğidir (FR-62). */
    static List<FieldErrorItem> missingRequired(ParticipantSurveyForm form, List<AnswerInput> answers) {
        Set<UUID> answered = answers.stream()
                .filter(answer -> !answer.isEmpty())
                .map(AnswerInput::questionId)
                .collect(Collectors.toSet());
        return form.questions().stream()
                .filter(question -> question.required() && !answered.contains(question.id()))
                .map(question -> error(question.id().toString(), "REQUIRED", "Bu soru zorunludur"))
                .toList();
    }

    static long answeredCount(List<AnswerInput> answers) {
        return answers.stream().filter(answer -> !answer.isEmpty()).count();
    }

    private static void likert(Question question, AnswerInput answer, String path, List<FieldErrorItem> errors) {
        onlyFields(answer, path, errors, true, false, false, false);
        Integer value = answer.numericValue();
        int scale = intSetting(question, "scale", 5);
        if (value != null && (value < 1 || value > scale)) {
            errors.add(error(path + ".numericValue", "OUT_OF_RANGE", "1 ile " + scale + " arasında olmalı"));
        }
    }

    private static void choice(Question question, AnswerInput answer, String path, List<FieldErrorItem> errors,
                               boolean single) {
        onlyFields(answer, path, errors, false, true, true, false);
        Set<UUID> allowed = question.options().stream().map(ParticipantSurveyForm.Option::id).collect(Collectors.toSet());
        List<UUID> selected = answer.optionIds() == null ? List.of() : answer.optionIds();
        Set<UUID> unique = new HashSet<>();
        for (int j = 0; j < selected.size(); j++) {
            UUID optionId = selected.get(j);
            if (!allowed.contains(optionId)) {
                errors.add(error(path + ".optionIds[" + j + "]", "UNKNOWN_OPTION", "Bu seçenek bu soruya ait değil"));
            } else if (!unique.add(optionId)) {
                errors.add(error(path + ".optionIds[" + j + "]", "DUPLICATE_OPTION", "Aynı seçenek birden fazla kez seçildi"));
            }
        }

        boolean hasOther = answer.otherText() != null && !answer.otherText().isBlank();
        if (hasOther && !boolSetting(question, "allowOther")) {
            errors.add(error(path + ".otherText", "OTHER_NOT_ALLOWED", "Bu soruda \"Diğer\" seçeneği yok"));
        }

        int count = unique.size() + (hasOther ? 1 : 0);
        if (single) {
            if (count > 1) {
                errors.add(error(path + ".optionIds", "TOO_MANY_SELECTIONS", "Yalnızca bir seçim yapılabilir"));
            }
            return;
        }
        int min = intSetting(question, "minSelect", 0);
        Integer max = question.settings() != null && question.settings().get("maxSelect") instanceof Number n
                ? n.intValue() : null;
        if (count < min) {
            errors.add(error(path + ".optionIds", "TOO_FEW_SELECTIONS", "En az " + min + " seçim yapılmalı"));
        }
        if (max != null && count > max) {
            errors.add(error(path + ".optionIds", "TOO_MANY_SELECTIONS", "En fazla " + max + " seçim yapılabilir"));
        }
    }

    private static void freeText(Question question, AnswerInput answer, String path, List<FieldErrorItem> errors) {
        onlyFields(answer, path, errors, false, false, false, true);
        int maxLength = intSetting(question, "maxLength", 1000);
        if (answer.text() != null && answer.text().length() > maxLength) {
            errors.add(error(path + ".text", "TOO_LONG", "En fazla " + maxLength + " karakter olabilir"));
        }
    }

    /** Soru tipine ait olmayan alanlar dolu gelemez. */
    private static void onlyFields(AnswerInput answer, String path, List<FieldErrorItem> errors,
                                   boolean numeric, boolean options, boolean other, boolean text) {
        if (!numeric && answer.numericValue() != null) {
            errors.add(error(path + ".numericValue", "NOT_ALLOWED", "Bu soru tipinde kullanılamaz"));
        }
        if (!options && answer.optionIds() != null && !answer.optionIds().isEmpty()) {
            errors.add(error(path + ".optionIds", "NOT_ALLOWED", "Bu soru tipinde kullanılamaz"));
        }
        if (!other && answer.otherText() != null && !answer.otherText().isBlank()) {
            errors.add(error(path + ".otherText", "NOT_ALLOWED", "Bu soru tipinde kullanılamaz"));
        }
        if (!text && answer.text() != null && !answer.text().isBlank()) {
            errors.add(error(path + ".text", "NOT_ALLOWED", "Bu soru tipinde kullanılamaz"));
        }
    }

    private static int intSetting(Question question, String key, int fallback) {
        return question.settings() != null && question.settings().get(key) instanceof Number n ? n.intValue() : fallback;
    }

    private static boolean boolSetting(Question question, String key) {
        return question.settings() != null && Boolean.TRUE.equals(question.settings().get(key));
    }

    private static FieldErrorItem error(String field, String code, String message) {
        return new FieldErrorItem(field, code, message);
    }
}
