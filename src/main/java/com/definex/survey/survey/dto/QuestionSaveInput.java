package com.definex.survey.survey.dto;

import com.definex.survey.survey.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Taslakta {@code text} ve {@code type} dahil tüm alanlar boş olabilir.
 *
 * @param id       mevcut soru için verilir; yeni soruda boş bırakılır
 * @param required verilmezse {@code false}
 * @param settings tipe özel ayarlar; {@code type} yoksa gönderilemez
 * @param options  yalnızca {@code SINGLE_CHOICE} ve {@code MULTIPLE_CHOICE} için; dizi sırası = seçenek sırası
 */
public record QuestionSaveInput(
        UUID id,
        @Size(max = 1000) String text,
        QuestionType type,
        Boolean required,
        Map<String, Object> settings,
        @Valid @Size(max = 100) List<OptionSaveInput> options) {
}
