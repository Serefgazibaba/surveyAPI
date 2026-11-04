package com.definex.survey.template.service;

import com.definex.survey.exception.ApiException;
import com.definex.survey.exception.FieldErrorItem;
import com.definex.survey.survey.service.QuestionSettingsValidator;
import com.definex.survey.template.dto.TemplateOption;
import com.definex.survey.template.dto.TemplateQuestion;
import com.definex.survey.template.dto.TemplateQuestionInput;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Şablon sorularını anket taslağıyla aynı kurallarla doğrular: ayarlar tipe göre kontrol edilip varsayılanlarla
 * tamamlanır (400), seçenekler yalnızca seçimli tiplerde kabul edilir (422). Tamlık (metin, tip, en az 2 seçenek)
 * kontrol edilmez; şablondan üretilen anket yayınlanırken kontrol edilir.
 */
@Component
class TemplateContentValidator {

    private final QuestionSettingsValidator settingsValidator;

    TemplateContentValidator(QuestionSettingsValidator settingsValidator) {
        this.settingsValidator = settingsValidator;
    }

    List<TemplateQuestion> toContent(List<TemplateQuestionInput> inputs) {
        List<FieldErrorItem> formatErrors = new ArrayList<>();
        List<FieldErrorItem> structureErrors = new ArrayList<>();
        List<TemplateQuestion> content = new ArrayList<>();

        for (int i = 0; i < inputs.size(); i++) {
            TemplateQuestionInput in = inputs.get(i);
            String path = "questions[" + i + "]";
            Map<String, Object> settings =
                    settingsValidator.normalize(in.type(), in.settings(), path + ".settings", formatErrors);
            if (!in.optionsOrEmpty().isEmpty() && (in.type() == null || !in.type().hasOptions())) {
                structureErrors.add(new FieldErrorItem(path + ".options", "NOT_ALLOWED",
                        "Seçenekler yalnızca SINGLE_CHOICE ve MULTIPLE_CHOICE sorularında gönderilir"));
            }
            List<TemplateOption> options = in.optionsOrEmpty().stream()
                    .map(option -> new TemplateOption(option.label()))
                    .toList();
            content.add(new TemplateQuestion(in.text(), in.type(), Boolean.TRUE.equals(in.required()), settings, options));
        }

        if (!formatErrors.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "İstek doğrulanamadı", formatErrors);
        }
        if (!structureErrors.isEmpty()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_STRUCTURE", "Şablon yapısı geçersiz",
                    structureErrors);
        }
        return content;
    }
}
