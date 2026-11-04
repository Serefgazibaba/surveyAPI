package com.definex.survey.survey.dto;

import com.definex.survey.survey.entity.ResponseMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Anket kaydetme (upsert). {@code id} yoksa yeni anket oluşturulur, varsa güncellenir.
 * Kaydetmede yalnızca {@code title} zorunludur; tamlık yayınlamada kontrol edilir.
 *
 * @param questions dizi sırası = soru sırası; listede olmayan mevcut sorular silinir
 */
public record SurveySaveRequest(
        UUID id,
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        Instant endsAt,
        ResponseMode responseMode,
        @Valid @Size(max = 200) List<QuestionSaveInput> questions) {

    public List<QuestionSaveInput> questionsOrEmpty() {
        return questions == null ? List.of() : questions;
    }
}
