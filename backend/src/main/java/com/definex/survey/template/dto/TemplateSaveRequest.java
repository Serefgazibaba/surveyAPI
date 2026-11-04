package com.definex.survey.template.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Şablon upsert. {@code id} yoksa yeni şablon oluşturulur, varsa güncellenir.
 *
 * @param questions şablonun sorularının tamamı; mevcut içerik bu listeyle değiştirilir (dizi sırası = soru sırası)
 */
public record TemplateSaveRequest(
        UUID id,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @Valid @Size(max = 200) List<TemplateQuestionInput> questions) {

    public List<TemplateQuestionInput> questionsOrEmpty() {
        return questions == null ? List.of() : questions;
    }
}
