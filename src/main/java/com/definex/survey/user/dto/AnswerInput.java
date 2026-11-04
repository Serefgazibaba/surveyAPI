package com.definex.survey.user.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Tek bir sorunun cevabı. Soru tipine göre yalnızca ilgili alan doldurulur:
 * LIKERT → {@code numericValue}; SINGLE/MULTIPLE_CHOICE → {@code optionIds} ve/veya {@code otherText};
 * FREE_TEXT → {@code text}. Hiçbir alanı dolu olmayan cevap "yanıtlanmamış" sayılır.
 */
public record AnswerInput(
        @NotNull UUID questionId,
        Integer numericValue,
        @Size(max = 100) List<@NotNull UUID> optionIds,
        @Size(max = 500) String otherText,
        @Size(max = 5000) String text) {

    @JsonIgnore
    public boolean isEmpty() {
        return numericValue == null
                && (optionIds == null || optionIds.isEmpty())
                && (otherText == null || otherText.isBlank())
                && (text == null || text.isBlank());
    }
}
