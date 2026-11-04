package com.definex.survey.template.dto;

import com.definex.survey.survey.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

/**
 * Şablon sorusu girdisi. Anket taslağındaki gibi tüm alanlar boş olabilir.
 *
 * @param required verilmezse {@code false}
 * @param settings tipe özel ayarlar; {@code type} yoksa gönderilemez
 * @param options  yalnızca {@code SINGLE_CHOICE} ve {@code MULTIPLE_CHOICE} için; dizi sırası = seçenek sırası
 */
public record TemplateQuestionInput(
        @Size(max = 1000) String text,
        QuestionType type,
        Boolean required,
        Map<String, Object> settings,
        @Valid @Size(max = 100) List<TemplateOptionInput> options) {

    public List<TemplateOptionInput> optionsOrEmpty() {
        return options == null ? List.of() : options;
    }
}
