package com.definex.survey.template.dto;

import com.definex.survey.survey.entity.QuestionType;
import java.util.List;
import java.util.Map;

/**
 * Şablon içeriğinde ({@code templates.content}, jsonb) saklanan soru. Kimlik taşımaz; şablondan anket
 * üretilirken yeni soru ve seçenekler oluşturulur.
 *
 * @param settings varsayılanlarla tamamlanmış tipe özel ayarlar; tip yoksa {@code null}
 */
public record TemplateQuestion(
        String text,
        QuestionType type,
        boolean required,
        Map<String, Object> settings,
        List<TemplateOption> options) {

    public List<TemplateOption> optionsOrEmpty() {
        return options == null ? List.of() : options;
    }
}
