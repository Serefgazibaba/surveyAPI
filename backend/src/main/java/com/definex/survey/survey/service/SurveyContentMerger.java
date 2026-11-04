package com.definex.survey.survey.service;

import com.definex.survey.exception.ApiException;
import com.definex.survey.exception.FieldErrorItem;
import com.definex.survey.survey.dto.OptionSaveInput;
import com.definex.survey.survey.dto.QuestionSaveInput;
import com.definex.survey.survey.dto.SurveySaveRequest;
import com.definex.survey.survey.entity.Question;
import com.definex.survey.survey.entity.QuestionOption;
import com.definex.survey.survey.entity.Survey;
import com.definex.survey.survey.entity.SurveyStatus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Kaydetme isteğini mevcut anket ağacına uygular (upsert).
 * <ul>
 *   <li>{@code id}'li soru/seçenek güncellenir, {@code id}'siz olan eklenir, listede olmayan silinir.</li>
 *   <li>Sıra, dizideki sıradır.</li>
 *   <li>{@code PUBLISHED} ankette yalnızca metinler değişebilir (BR-04, BR-05).</li>
 * </ul>
 * Durum kontrolü ({@code SURVEY_NOT_EDITABLE}) çağıran tarafta yapılır.
 */
@Component
public class SurveyContentMerger {

    private final QuestionSettingsValidator settingsValidator;

    public SurveyContentMerger(QuestionSettingsValidator settingsValidator) {
        this.settingsValidator = settingsValidator;
    }

    public void apply(Survey survey, SurveySaveRequest request) {
        List<QuestionSaveInput> inputs = request.questionsOrEmpty();
        List<Map<String, Object>> settings = normalizeSettings(inputs);
        validateStructure(survey, inputs);

        if (survey.getStatus() == SurveyStatus.PUBLISHED) {
            applyToPublished(survey, request, inputs, settings);
        } else {
            applyToDraft(survey, request, inputs, settings);
        }
    }

    // ---------------------------------------------------------------- doğrulama

    /** Biçim hataları (400). */
    private List<Map<String, Object>> normalizeSettings(List<QuestionSaveInput> inputs) {
        List<FieldErrorItem> errors = new ArrayList<>();
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            QuestionSaveInput in = inputs.get(i);
            result.add(settingsValidator.normalize(in.type(), in.settings(), "questions[" + i + "].settings", errors));
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "İstek doğrulanamadı", errors);
        }
        return result;
    }

    /** Kimlik ve yapı hataları (422): tekrarlanan id, bu ankete ait olmayan id, seçimli olmayan tipe seçenek. */
    private void validateStructure(Survey survey, List<QuestionSaveInput> inputs) {
        Map<UUID, Question> existing = byId(survey.getQuestions(), Question::getId);
        List<FieldErrorItem> errors = new ArrayList<>();
        Set<UUID> seenQuestions = new HashSet<>();

        for (int i = 0; i < inputs.size(); i++) {
            QuestionSaveInput in = inputs.get(i);
            String path = "questions[" + i + "]";
            Question current = null;
            if (in.id() != null) {
                if (!seenQuestions.add(in.id())) {
                    errors.add(new FieldErrorItem(path + ".id", "DUPLICATE_ID", "Aynı soru birden fazla kez gönderildi"));
                } else if (!existing.containsKey(in.id())) {
                    errors.add(new FieldErrorItem(path + ".id", "UNKNOWN_ID", "Bu soru bu ankete ait değil"));
                } else {
                    current = existing.get(in.id());
                }
            }

            List<OptionSaveInput> options = optionsOf(in);
            if (!options.isEmpty() && (in.type() == null || !in.type().hasOptions())) {
                errors.add(new FieldErrorItem(path + ".options", "NOT_ALLOWED",
                        "Seçenekler yalnızca SINGLE_CHOICE ve MULTIPLE_CHOICE sorularında gönderilir"));
                continue;
            }
            Map<UUID, QuestionOption> existingOptions =
                    current == null ? Map.of() : byId(current.getOptions(), QuestionOption::getId);
            Set<UUID> seenOptions = new HashSet<>();
            for (int j = 0; j < options.size(); j++) {
                UUID optionId = options.get(j).id();
                String optionPath = path + ".options[" + j + "].id";
                if (optionId == null) {
                    continue;
                }
                if (!seenOptions.add(optionId)) {
                    errors.add(new FieldErrorItem(optionPath, "DUPLICATE_ID", "Aynı seçenek birden fazla kez gönderildi"));
                } else if (!existingOptions.containsKey(optionId)) {
                    errors.add(new FieldErrorItem(optionPath, "UNKNOWN_ID", "Bu seçenek bu soruya ait değil"));
                }
            }
        }
        if (!errors.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_STRUCTURE", "Anket yapısı geçersiz", errors);
        }
    }

    // ---------------------------------------------------------------- taslak

    private void applyToDraft(Survey survey, SurveySaveRequest request, List<QuestionSaveInput> inputs,
                              List<Map<String, Object>> settings) {
        survey.updateDetails(request.title(), request.description(), request.endsAt(), request.responseMode());

        Map<UUID, Question> existing = byId(survey.getQuestions(), Question::getId);
        List<Question> ordered = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            QuestionSaveInput in = inputs.get(i);
            Question question = in.id() == null ? new Question(survey) : existing.get(in.id());
            question.update(in.text(), in.type(), Boolean.TRUE.equals(in.required()), settings.get(i));
            question.replaceOptions(in.type() != null && in.type().hasOptions()
                    ? mergeOptions(question, optionsOf(in))
                    : List.of());
            ordered.add(question);
        }
        survey.replaceQuestions(ordered);
    }

    private List<QuestionOption> mergeOptions(Question question, List<OptionSaveInput> inputs) {
        Map<UUID, QuestionOption> existing = byId(question.getOptions(), QuestionOption::getId);
        List<QuestionOption> ordered = new ArrayList<>();
        for (OptionSaveInput in : inputs) {
            QuestionOption option = in.id() == null ? new QuestionOption(question) : existing.get(in.id());
            option.updateLabel(in.label());
            ordered.add(option);
        }
        return ordered;
    }

    // ---------------------------------------------------------------- yayında

    private void applyToPublished(Survey survey, SurveySaveRequest request, List<QuestionSaveInput> inputs,
                                  List<Map<String, Object>> settings) {
        if (!sameStructure(survey, request, inputs, settings)) {
            throw new ApiException(HttpStatus.CONFLICT, "SURVEY_STRUCTURE_LOCKED",
                    "Yayındaki ankette yalnızca metinler değiştirilebilir");
        }
        survey.updateTexts(request.title(), request.description());
        List<Question> questions = survey.getQuestions();
        for (int i = 0; i < inputs.size(); i++) {
            Question question = questions.get(i);
            question.updateText(inputs.get(i).text());
            List<OptionSaveInput> options = optionsOf(inputs.get(i));
            for (int j = 0; j < options.size(); j++) {
                question.getOptions().get(j).updateLabel(options.get(j).label());
            }
        }
    }

    /** Metin dışındaki her şey (sıra, id'ler, tip, zorunluluk, ayarlar, bitiş tarihi, yanıt modu) aynı mı? */
    private boolean sameStructure(Survey survey, SurveySaveRequest request, List<QuestionSaveInput> inputs,
                                  List<Map<String, Object>> settings) {
        if (!Objects.equals(survey.getEndsAt(), request.endsAt())
                || survey.getResponseMode() != request.responseMode()) {
            return false;
        }
        List<Question> questions = survey.getQuestions();
        if (questions.size() != inputs.size()) {
            return false;
        }
        for (int i = 0; i < inputs.size(); i++) {
            QuestionSaveInput in = inputs.get(i);
            Question question = questions.get(i);
            if (!question.getId().equals(in.id())
                    || question.getType() != in.type()
                    || question.isRequired() != Boolean.TRUE.equals(in.required())
                    || !Objects.equals(question.getSettings(), settings.get(i))) {
                return false;
            }
            List<OptionSaveInput> options = optionsOf(in);
            if (question.getOptions().size() != options.size()) {
                return false;
            }
            for (int j = 0; j < options.size(); j++) {
                if (!question.getOptions().get(j).getId().equals(options.get(j).id())) {
                    return false;
                }
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- yardımcılar

    private static List<OptionSaveInput> optionsOf(QuestionSaveInput in) {
        return in.options() == null ? List.of() : in.options();
    }

    private static <T> Map<UUID, T> byId(List<T> items, Function<T, UUID> id) {
        return items.stream().collect(Collectors.toMap(id, Function.identity(), (a, b) -> a, HashMap::new));
    }
}
